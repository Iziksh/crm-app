package com.crm.service;

import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ContractStatus;
import com.crm.domain.enums.LeadStatus;
import com.crm.domain.enums.OpportunityStage;
import com.crm.dto.response.DashboardStats;
import com.crm.repository.AccountRepository;
import com.crm.repository.ActivityRepository;
import com.crm.repository.ContactRepository;
import com.crm.repository.ContractRepository;
import com.crm.repository.LeadRepository;
import com.crm.repository.OpportunityRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class DashboardStatsService {

    private static final String CACHE_KEY = "global";

    private final AccountRepository accountRepository;
    private final ContactRepository contactRepository;
    private final OpportunityRepository opportunityRepository;
    private final LeadRepository leadRepository;
    private final ActivityRepository activityRepository;
    private final ContractRepository contractRepository;
    private final Cache<String, DashboardStats> cache;

    public DashboardStatsService(AccountRepository accountRepository,
                                 ContactRepository contactRepository,
                                 OpportunityRepository opportunityRepository,
                                 LeadRepository leadRepository,
                                 ActivityRepository activityRepository,
                                 ContractRepository contractRepository,
                                 @Value("${app.dashboard.cache-ttl-seconds:45}") int cacheTtlSeconds) {
        this.accountRepository = accountRepository;
        this.contactRepository = contactRepository;
        this.opportunityRepository = opportunityRepository;
        this.leadRepository = leadRepository;
        this.activityRepository = activityRepository;
        this.contractRepository = contractRepository;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(cacheTtlSeconds, TimeUnit.SECONDS)
                .maximumSize(1)
                .build();
    }

    @Transactional(readOnly = true)
    public DashboardStats getStats() {
        return cache.get(CACHE_KEY, key -> loadStats());
    }

    public void invalidateCache() {
        cache.invalidateAll();
    }

    @Transactional(readOnly = true)
    public DashboardStats loadStats() {
        return new DashboardStats(
                accountRepository.count(),
                contactRepository.count(),
                nullToZero(opportunityRepository.sumPipelineAmount()),
                contractRepository.countByEndDateBefore(LocalDate.now().plusDays(30)),
                toEnumMap(opportunityRepository.countGroupByStage(), OpportunityStage.class),
                toEnumMap(leadRepository.countGroupByStatus(), LeadStatus.class),
                toEnumMap(activityRepository.countGroupByStatus(), ActivityStatus.class),
                toEnumMap(contractRepository.countGroupByStatus(), ContractStatus.class)
        );
    }

    private static BigDecimal nullToZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static <E extends Enum<E>> Map<E, Long> toEnumMap(List<Object[]> rows, Class<E> enumType) {
        Map<E, Long> map = new EnumMap<>(enumType);
        for (E constant : enumType.getEnumConstants()) {
            map.put(constant, 0L);
        }
        for (Object[] row : rows) {
            @SuppressWarnings("unchecked")
            E key = (E) row[0];
            map.put(key, (Long) row[1]);
        }
        return map;
    }
}
