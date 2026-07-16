package com.crm.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LineItemRequest(
        Long id,
        @NotBlank String productOrService,
        String description,
        @NotNull BigDecimal quantity,
        @NotNull BigDecimal unitPrice,
        Integer sortOrder
) {}
