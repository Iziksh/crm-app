package com.crm.billing.dto;

import com.crm.billing.enums.DiscountType;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.enums.RoundingMode;
import com.crm.billing.enums.VatType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TaxDocumentCreateRequest(
        @NotNull DocumentType documentType,
        @NotNull Long accountId,
        String currency,
        LocalDate documentDate,
        String freeText,
        VatType vatType,
        DiscountType discountType,
        BigDecimal discountValue,
        RoundingMode roundingMode,
        @NotEmpty @Valid List<LineItemRequest> lineItems
) {}
