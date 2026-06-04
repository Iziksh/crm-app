package com.crm.service;

import com.crm.domain.entity.Opportunity;
import com.crm.domain.enums.OpportunityStage;
import com.crm.dto.request.OpportunityRequest;
import com.crm.dto.response.OpportunityResponse;
import com.crm.dto.response.QuoteResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.QuoteRepository;
import com.crm.repository.AccountRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.OpportunityRepository;
import com.crm.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;
    private final LeadRepository leadRepository;
    private final QuoteRepository quoteRepository;
    private final NotificationService notificationService;
    private final CrmEventPublisher eventPublisher;

    public OpportunityService(OpportunityRepository opportunityRepository,
                              UserRepository userRepository,
                              AccountRepository accountRepository,
                              ContactRepository contactRepository,
                              LeadRepository leadRepository,
                              QuoteRepository quoteRepository,
                              NotificationService notificationService,
                              CrmEventPublisher eventPublisher) {
        this.opportunityRepository = opportunityRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
        this.leadRepository = leadRepository;
        this.quoteRepository = quoteRepository;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
    }

    public OpportunityResponse create(OpportunityRequest request, String createdByUsername) {
        Opportunity opp = mapToEntity(new Opportunity(), request);
        userRepository.findByUsername(createdByUsername).ifPresent(opp::setCreatedBy);
        Opportunity saved = opportunityRepository.save(opp);
        eventPublisher.publishCreated("OPPORTUNITY", saved.getId());
        return OpportunityResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public OpportunityResponse findById(Long id) {
        return OpportunityResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<OpportunityResponse> findAll(Pageable pageable) {
        return opportunityRepository.findAll(pageable).map(OpportunityResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OpportunityResponse> findAll(Pageable pageable, OpportunityStage stage, String search) {
        return opportunityRepository.findAll(buildSpec(stage, search), pageable).map(OpportunityResponse::from);
    }

    @Transactional(readOnly = true)
    public long count(OpportunityStage stage, String search) {
        return opportunityRepository.count(buildSpec(stage, search));
    }

    private Specification<Opportunity> buildSpec(OpportunityStage stage, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (stage != null) predicates.add(cb.equal(root.get("stage"), stage));
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> findAllForExport(OpportunityStage stage, String search) {
        return opportunityRepository.findAll(buildSpec(stage, search)).stream().map(OpportunityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OpportunityResponse> findByStage(OpportunityStage stage) {
        return opportunityRepository.findByStage(stage).stream().map(OpportunityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long countByStage(OpportunityStage stage) {
        return opportunityRepository.countByStage(stage);
    }

    @Transactional(readOnly = true)
    public BigDecimal sumPipelineAmount() {
        BigDecimal sum = opportunityRepository.sumPipelineAmount();
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public List<QuoteResponse> findQuotes(Long opportunityId) {
        return quoteRepository.findByOpportunity_Id(opportunityId).stream().map(QuoteResponse::from).toList();
    }

    public OpportunityResponse update(Long id, OpportunityRequest request) {
        Opportunity opp = getOrThrow(id);
        boolean becomingWon = request.stage() == OpportunityStage.WON && opp.getStage() != OpportunityStage.WON;
        Opportunity saved = opportunityRepository.save(mapToEntity(opp, request));
        if (becomingWon && saved.getAssignedTo() != null) {
            notificationService.notify(saved.getAssignedTo().getId(),
                    "Opportunity WON: " + saved.getName(), "OPPORTUNITY", saved.getId());
        }
        eventPublisher.publishUpdated("OPPORTUNITY", id,
                java.util.Map.of("stage", saved.getStage().name()));
        return OpportunityResponse.from(saved);
    }

    public void delete(Long id) {
        opportunityRepository.delete(getOrThrow(id));
        eventPublisher.publishDeleted("OPPORTUNITY", id);
    }

    private Opportunity getOrThrow(Long id) {
        return opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity", "id", id));
    }

    private Opportunity mapToEntity(Opportunity opp, OpportunityRequest request) {
        opp.setName(request.name());
        opp.setAmount(request.amount());
        opp.setCurrency(request.currency() != null ? request.currency() : "USD");
        opp.setCloseDate(request.closeDate());
        opp.setNotes(request.notes());
        if (request.stage() != null) opp.setStage(request.stage());
        if (request.probability() != null) opp.setProbability(request.probability());
        if (request.assignedToId() != null) {
            opp.setAssignedTo(userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assignedToId())));
        } else {
            opp.setAssignedTo(null);
        }
        if (request.accountId() != null) {
            opp.setAccount(accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.accountId())));
        } else {
            opp.setAccount(null);
        }
        if (request.contactId() != null) {
            opp.setContact(contactRepository.findById(request.contactId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", request.contactId())));
        } else {
            opp.setContact(null);
        }
        if (request.leadId() != null) {
            opp.setLead(leadRepository.findById(request.leadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", request.leadId())));
        } else {
            opp.setLead(null);
        }
        return opp;
    }
}
