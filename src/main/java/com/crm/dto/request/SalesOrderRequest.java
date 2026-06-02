package com.crm.dto.request;

import com.crm.domain.enums.SalesOrderStatus;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record SalesOrderRequest(
        @NotBlank String title,
        SalesOrderStatus status,
        LocalDate orderDate,
        LocalDate deliveryDate,
        String currency,
        String notes,
        Long quoteId,
        Long accountId,
        Long contactId,
        Long assignedToId,
        List<SalesOrderLineItemRequest> lineItems
) {}
