package com.crm.billing.service;

import com.crm.billing.dto.LineItemRequest;
import com.crm.billing.dto.TotalsBreakdown;
import com.crm.billing.enums.DiscountType;
import com.crm.billing.enums.RoundingMode;
import com.crm.billing.enums.VatType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * net (sum of line items) -> discount (%/₪) -> VAT per rate -> rounding -> gross.
 * BigDecimal scale 2, {@link java.math.RoundingMode#HALF_UP} throughout, unless the document's
 * RoundingMode dictates a coarser final step. No existing house convention for money in this codebase
 * to reuse (Quote/SalesOrder line items and ForecastService use inconsistent ad-hoc scales) — this
 * class is the standard for the Billing & Documents addon.
 */
@Component
public class TotalsCalculator {

    private static final int SCALE = 2;
    private static final java.math.RoundingMode HALF_UP = java.math.RoundingMode.HALF_UP;

    /** Cash-rounding increment for {@link RoundingMode#TO_AGOROT}. Assumption: mirrors Israel's
     * post-2008 cash-rounding law (round to nearest 10 agorot), since "עיגול לאגורות" is ambiguous
     * without a real spec to copy from. */
    private static final BigDecimal AGOROT_INCREMENT = new BigDecimal("0.10");

    private final BigDecimal standardVatRate;

    public TotalsCalculator(@Value("${tax.vat.standard-rate:0.18}") BigDecimal standardVatRate) {
        this.standardVatRate = standardVatRate;
    }

    public TotalsBreakdown calculate(List<LineItemRequest> lineItems,
                                      DiscountType discountType,
                                      BigDecimal discountValue,
                                      VatType vatType,
                                      RoundingMode roundingMode) {
        BigDecimal netBeforeDiscount = lineItems.stream()
                .map(li -> li.quantity().multiply(li.unitPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, HALF_UP);

        BigDecimal discountAmount = computeDiscount(netBeforeDiscount, discountType, discountValue);
        BigDecimal netTotal = netBeforeDiscount.subtract(discountAmount).setScale(SCALE, HALF_UP);

        BigDecimal vatRate = vatRateFor(vatType);
        BigDecimal vatAmount = netTotal.multiply(vatRate).setScale(SCALE, HALF_UP);

        BigDecimal preRoundTotal = netTotal.add(vatAmount).setScale(SCALE, HALF_UP);
        BigDecimal grossTotal = applyRounding(preRoundTotal, roundingMode);
        BigDecimal roundingDelta = preRoundTotal.subtract(grossTotal).setScale(SCALE, HALF_UP);

        return new TotalsBreakdown(netBeforeDiscount, discountAmount, netTotal, vatRate, vatAmount,
                preRoundTotal, roundingDelta, grossTotal);
    }

    /** net = gross / (1 + rate), used when a total is known gross-inclusive and the net/VAT split
     * must be reconstructed (e.g. previewing totals for a legacy gross-only amount). */
    public BigDecimal extractNet(BigDecimal gross, BigDecimal vatRate) {
        return gross.divide(BigDecimal.ONE.add(vatRate), SCALE, HALF_UP);
    }

    private BigDecimal computeDiscount(BigDecimal netBeforeDiscount, DiscountType discountType, BigDecimal discountValue) {
        if (discountType == null || discountValue == null || discountValue.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = discountType == DiscountType.PERCENT
                ? netBeforeDiscount.multiply(discountValue).divide(BigDecimal.valueOf(100), SCALE, HALF_UP)
                : discountValue;
        // Clamp so a discount can never exceed the pre-discount net (would otherwise flip totals negative).
        return amount.min(netBeforeDiscount).setScale(SCALE, HALF_UP);
    }

    private BigDecimal vatRateFor(VatType vatType) {
        if (vatType == null || vatType == VatType.STANDARD) {
            return standardVatRate;
        }
        return BigDecimal.ZERO; // ZERO and EXEMPT both carry no VAT amount, distinguished only for reporting.
    }

    private BigDecimal applyRounding(BigDecimal preRoundTotal, RoundingMode roundingMode) {
        if (roundingMode == null) {
            roundingMode = RoundingMode.NONE;
        }
        return switch (roundingMode) {
            case NONE -> preRoundTotal;
            case TO_AGOROT -> roundToIncrement(preRoundTotal, AGOROT_INCREMENT);
            case TO_SHEKEL -> preRoundTotal.setScale(0, HALF_UP).setScale(SCALE, HALF_UP);
        };
    }

    private BigDecimal roundToIncrement(BigDecimal value, BigDecimal increment) {
        return value.divide(increment, 0, HALF_UP).multiply(increment).setScale(SCALE, HALF_UP);
    }
}
