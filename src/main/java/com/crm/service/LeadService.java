package com.crm.service;

import com.crm.domain.entity.Account;
import com.crm.domain.entity.Contact;
import com.crm.domain.entity.Lead;
import com.crm.domain.entity.Opportunity;
import com.crm.domain.enums.ContactStatus;
import com.crm.domain.enums.LeadStatus;
import com.crm.domain.enums.OpportunityStage;
import com.crm.dto.request.LeadRequest;
import com.crm.dto.response.LeadResponse;
import com.crm.dto.response.OpportunityResponse;
import com.crm.exception.ResourceNotFoundException;
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

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class LeadService {

    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;

    public LeadService(LeadRepository leadRepository,
                       OpportunityRepository opportunityRepository,
                       UserRepository userRepository,
                       AccountRepository accountRepository,
                       ContactRepository contactRepository) {
        this.leadRepository = leadRepository;
        this.opportunityRepository = opportunityRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
    }

    public LeadResponse create(LeadRequest request, String createdByUsername) {
        Lead lead = mapToEntity(new Lead(), request);
        userRepository.findByUsername(createdByUsername).ifPresent(lead::setCreatedBy);
        return LeadResponse.from(leadRepository.save(lead));
    }

    @Transactional(readOnly = true)
    public LeadResponse findById(Long id) {
        return LeadResponse.from(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<LeadResponse> findAll(Pageable pageable) {
        return leadRepository.findAll(pageable).map(LeadResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<LeadResponse> findAll(Pageable pageable, LeadStatus status, String search) {
        return leadRepository.findAll(buildSpec(status, search), pageable).map(LeadResponse::from);
    }

    @Transactional(readOnly = true)
    public long count(LeadStatus status, String search) {
        return leadRepository.count(buildSpec(status, search));
    }

    private Specification<Lead> buildSpec(LeadStatus status, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("company")), pattern)
                ));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Transactional(readOnly = true)
    public List<LeadResponse> findByStatus(LeadStatus status) {
        return leadRepository.findByStatus(status).stream().map(LeadResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long countByStatus(LeadStatus status) {
        return leadRepository.countByStatus(status);
    }

    public LeadResponse update(Long id, LeadRequest request) {
        Lead lead = getOrThrow(id);
        return LeadResponse.from(leadRepository.save(mapToEntity(lead, request)));
    }

    public void delete(Long id) {
        leadRepository.delete(getOrThrow(id));
    }

    public OpportunityResponse convert(Long leadId) {
        Lead lead = getOrThrow(leadId);

        if (lead.getContact() == null && lead.getFirstName() != null && lead.getEmail() != null) {
            Contact contact = new Contact();
            contact.setFirstName(lead.getFirstName() != null ? lead.getFirstName() : "Unknown");
            contact.setLastName(lead.getLastName() != null ? lead.getLastName() : "Unknown");
            contact.setEmail(lead.getEmail());
            contact.setPhone(lead.getPhone());
            contact.setStatus(ContactStatus.ACTIVE);
            lead.setContact(contactRepository.save(contact));
        }

        if (lead.getAccount() == null && lead.getCompany() != null && !lead.getCompany().isBlank()) {
            Account account = new Account();
            account.setName(lead.getCompany());
            lead.setAccount(accountRepository.save(account));
        }

        Opportunity opp = new Opportunity();
        opp.setName(lead.getTitle());
        opp.setStage(OpportunityStage.PROSPECTING);
        opp.setLead(lead);
        opp.setAccount(lead.getAccount());
        opp.setContact(lead.getContact());
        opp.setAssignedTo(lead.getAssignedTo());
        opp.setCreatedBy(lead.getCreatedBy());
        if (lead.getEstimatedValue() != null) opp.setAmount(lead.getEstimatedValue());
        if (lead.getCurrency() != null) opp.setCurrency(lead.getCurrency());
        if (lead.getCloseDate() != null) opp.setCloseDate(lead.getCloseDate());

        lead.setStatus(LeadStatus.QUALIFIED);
        leadRepository.save(lead);

        return OpportunityResponse.from(opportunityRepository.save(opp));
    }

    private Lead getOrThrow(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", "id", id));
    }

    private Lead mapToEntity(Lead lead, LeadRequest request) {
        lead.setTitle(request.title());
        lead.setFirstName(request.firstName());
        lead.setLastName(request.lastName());
        lead.setEmail(request.email());
        lead.setPhone(request.phone());
        lead.setCompany(request.company());
        lead.setEstimatedValue(request.estimatedValue());
        lead.setCurrency(request.currency() != null ? request.currency() : "USD");
        lead.setCloseDate(request.closeDate());
        lead.setNotes(request.notes());
        if (request.status() != null) lead.setStatus(request.status());
        if (request.source() != null) lead.setSource(request.source());
        if (request.assignedToId() != null) {
            lead.setAssignedTo(userRepository.findById(request.assignedToId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.assignedToId())));
        } else {
            lead.setAssignedTo(null);
        }
        if (request.accountId() != null) {
            lead.setAccount(accountRepository.findById(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Account", "id", request.accountId())));
        } else {
            lead.setAccount(null);
        }
        if (request.contactId() != null) {
            lead.setContact(contactRepository.findById(request.contactId())
                    .orElseThrow(() -> new ResourceNotFoundException("Contact", "id", request.contactId())));
        } else {
            lead.setContact(null);
        }
        return lead;
    }
}
