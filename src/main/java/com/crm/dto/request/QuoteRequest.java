package com.crm.dto.request;

import com.crm.domain.enums.QuoteStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record QuoteRequest(
        @NotBlank String title,
        QuoteStatus status,
        LocalDate validUntil,
        String currency,
        String notes,
        Long opportunityId,
        Long accountId,
        Long contactId,
        Long assignedToId,
        List<QuoteLineItemRequest> lineItems
) {}
