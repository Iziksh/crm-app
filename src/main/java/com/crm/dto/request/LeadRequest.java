package com.crm.dto.request;

import com.crm.domain.enums.LeadSource;
import com.crm.domain.enums.LeadStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LeadRequest(
        @NotBlank String title,
        String firstName,
        String lastName,
        @Email String email,
        String phone,
        String company,
        LeadStatus status,
        LeadSource source,
        BigDecimal estimatedValue,
        String currency,
        LocalDate closeDate,
        String notes,
        Long assignedToId,
        Long accountId,
        Long contactId
) {}
