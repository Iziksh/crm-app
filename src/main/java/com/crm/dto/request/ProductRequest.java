package com.crm.dto.request;

import com.crm.domain.enums.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String sku,
        @NotBlank String name,
        String description,
        ProductCategory category,
        @NotNull BigDecimal unitPrice,
        String currency
) {}
