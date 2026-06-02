package com.crm.dto.request;

import com.crm.domain.enums.ContractStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractRequest(
        @NotBlank String title,
        ContractStatus status,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalValue,
        String currency,
        String description,
        String terms,
        Long salesOrderId,
        Long accountId,
        Long contactId,
        Long assignedToId
) {}
