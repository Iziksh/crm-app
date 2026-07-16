package com.crm.billing.dto;

import com.crm.billing.enums.DiscountType;
import com.crm.billing.enums.RoundingMode;
import com.crm.billing.enums.VatType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

/** Live totals preview while editing — never persisted. */
public record TotalsPreviewRequest(
        @NotEmpty @Valid List<LineItemRequest> lineItems,
        DiscountType discountType,
        BigDecimal discountValue,
        VatType vatType,
        RoundingMode roundingMode
) {}
