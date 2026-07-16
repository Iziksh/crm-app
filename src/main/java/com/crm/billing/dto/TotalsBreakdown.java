package com.crm.billing.dto;

import java.math.BigDecimal;

/** Reproducible output of {@code TotalsCalculator}: net (after discount) -> VAT -> rounding -> gross. */
public record TotalsBreakdown(
        BigDecimal netBeforeDiscount,
        BigDecimal discountAmount,
        BigDecimal netTotal,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal preRoundTotal,
        BigDecimal roundingDelta,
        BigDecimal grossTotal
) {}
