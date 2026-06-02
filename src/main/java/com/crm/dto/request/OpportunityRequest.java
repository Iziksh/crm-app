package com.crm.dto.request;

import com.crm.domain.enums.OpportunityStage;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OpportunityRequest(
        @NotBlank String name,
        OpportunityStage stage,
        BigDecimal amount,
        String currency,
        Integer probability,
        LocalDate closeDate,
        String notes,
        Long leadId,
        Long accountId,
        Long contactId,
        Long assignedToId
) {}
