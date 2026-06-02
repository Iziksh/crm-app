package com.crm.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record SalesOrderLineItemRequest(
        @NotBlank String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountPct,
        Integer sortOrder
) {}
