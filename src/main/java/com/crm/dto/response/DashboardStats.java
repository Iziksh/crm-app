package com.crm.dto.response;

import com.crm.domain.enums.ActivityStatus;
import com.crm.domain.enums.ContractStatus;
import com.crm.domain.enums.LeadStatus;
import com.crm.domain.enums.OpportunityStage;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardStats(
        long accountCount,
        long contactCount,
        BigDecimal pipelineAmount,
        long expiringContracts,
        Map<OpportunityStage, Long> opportunityByStage,
        Map<LeadStatus, Long> leadByStatus,
        Map<ActivityStatus, Long> activityByStatus,
        Map<ContractStatus, Long> contractByStatus
) {
    public long opportunityCount(OpportunityStage stage) {
        return opportunityByStage.getOrDefault(stage, 0L);
    }

    public long leadCount(LeadStatus status) {
        return leadByStatus.getOrDefault(status, 0L);
    }

    public long activityCount(ActivityStatus status) {
        return activityByStatus.getOrDefault(status, 0L);
    }

    public long contractCount(ContractStatus status) {
        return contractByStatus.getOrDefault(status, 0L);
    }
}
