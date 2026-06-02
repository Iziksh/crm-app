package com.crm.service;

import com.crm.domain.entity.SavedSearch;
import com.crm.domain.enums.SavedSearchScope;
import com.crm.dto.request.SavedSearchRequest;
import com.crm.dto.response.SavedSearchResponse;
import com.crm.exception.ResourceNotFoundException;
import com.crm.repository.AccountRepository;
import com.crm.repository.ActivityRepository;
import com.crm.repository.AddressRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.OpportunityRepository;
import com.crm.repository.SavedSearchRepository;
import com.crm.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SavedSearchService {

    private final SavedSearchRepository savedSearchRepository;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;
    private final ActivityRepository activityRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final AddressRepository addressRepository;
    private final ObjectMapper objectMapper;

    public SavedSearchService(SavedSearchRepository savedSearchRepository,
                              UserRepository userRepository,
                              AccountRepository accountRepository,
                              ContactRepository contactRepository,
                              ActivityRepository activityRepository,
                              LeadRepository leadRepository,
                              OpportunityRepository opportunityRepository,
                              AddressRepository addressRepository,
                              ObjectMapper objectMapper) {
        this.savedSearchRepository = savedSearchRepository;
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
        this.activityRepository = activityRepository;
        this.leadRepository = leadRepository;
        this.opportunityRepository = opportunityRepository;
        this.addressRepository = addressRepository;
        this.objectMapper = objectMapper;
    }

    public SavedSearchResponse create(SavedSearchRequest request, String ownerUsername) {
        SavedSearch ss = new SavedSearch();
        ss.setName(request.name());
        ss.setScope(request.scope());
        ss.setFilterJson(request.filterJson());
        userRepository.findByUsername(ownerUsername).ifPresent(ss::setOwner);
        return SavedSearchResponse.from(savedSearchRepository.save(ss));
    }

    @Transactional(readOnly = true)
    public List<SavedSearchResponse> findAll() {
        return savedSearchRepository.findAll().stream().map(SavedSearchResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<SavedSearchResponse> findByScope(SavedSearchScope scope) {
        return savedSearchRepository.findByScope(scope).stream().map(SavedSearchResponse::from).toList();
    }

    public SavedSearchResponse update(Long id, SavedSearchRequest request) {
        SavedSearch ss = getOrThrow(id);
        ss.setName(request.name());
        ss.setScope(request.scope());
        ss.setFilterJson(request.filterJson());
        return SavedSearchResponse.from(savedSearchRepository.save(ss));
    }

    public void delete(Long id) {
        savedSearchRepository.delete(getOrThrow(id));
    }

    @Transactional(readOnly = true)
    public long execute(Long searchId) {
        SavedSearch ss = getOrThrow(searchId);
        Map<String, String> filters = parseFilters(ss.getFilterJson());
        Specification<?> spec = buildSpec(filters);

        return switch (ss.getScope()) {
            case ACCOUNT -> accountRepository.count((Specification) spec);
            case CONTACT -> contactRepository.count((Specification) spec);
            case ACTIVITY -> activityRepository.count((Specification) spec);
            case LEAD -> leadRepository.count((Specification) spec);
            case OPPORTUNITY -> opportunityRepository.count((Specification) spec);
            case ADDRESS -> addressRepository.count((Specification) spec);
            default -> 0L;
        };
    }

    private Map<String, String> parseFilters(String filterJson) {
        if (filterJson == null || filterJson.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(filterJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Specification<?> buildSpec(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                try {
                    predicates.add(cb.like(
                            cb.lower(root.get(entry.getKey()).as(String.class)),
                            "%" + entry.getValue().toLowerCase() + "%"
                    ));
                } catch (Exception ignored) {}
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private SavedSearch getOrThrow(Long id) {
        return savedSearchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SavedSearch", "id", id));
    }
}
