package com.crm.billing.service;

import com.crm.billing.dto.LineItemRequest;
import com.crm.billing.dto.TotalsBreakdown;
import com.crm.billing.enums.DiscountType;
import com.crm.billing.enums.RoundingMode;
import com.crm.billing.enums.VatType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TotalsCalculatorTest {

    private final TotalsCalculator calculator = new TotalsCalculator(new BigDecimal("0.18"));

    private List<LineItemRequest> lineItems(double qty, double unitPrice) {
        return List.of(new LineItemRequest(null, "Widget", null, BigDecimal.valueOf(qty), BigDecimal.valueOf(unitPrice), 0));
    }

    @Test
    void percentDiscount_appliesBeforeVat() {
        TotalsBreakdown b = calculator.calculate(lineItems(1, 100), DiscountType.PERCENT, BigDecimal.TEN,
                VatType.STANDARD, RoundingMode.NONE);

        assertThat(b.netBeforeDiscount()).isEqualByComparingTo("100.00");
        assertThat(b.discountAmount()).isEqualByComparingTo("10.00");
        assertThat(b.netTotal()).isEqualByComparingTo("90.00");
        assertThat(b.vatAmount()).isEqualByComparingTo("16.20");
        assertThat(b.grossTotal()).isEqualByComparingTo("106.20");
    }

    @Test
    void amountDiscount_clampedToNetBeforeDiscount() {
        TotalsBreakdown b = calculator.calculate(lineItems(1, 50), DiscountType.AMOUNT, BigDecimal.valueOf(999),
                VatType.STANDARD, RoundingMode.NONE);

        assertThat(b.discountAmount()).isEqualByComparingTo("50.00");
        assertThat(b.netTotal()).isEqualByComparingTo("0.00");
        assertThat(b.grossTotal()).isEqualByComparingTo("0.00");
    }

    @Test
    void vatAt18Percent_addAndExtract() {
        TotalsBreakdown b = calculator.calculate(lineItems(2, 50), null, null, VatType.STANDARD, RoundingMode.NONE);

        assertThat(b.vatRate()).isEqualByComparingTo("0.18");
        assertThat(b.netTotal()).isEqualByComparingTo("100.00");
        assertThat(b.vatAmount()).isEqualByComparingTo("18.00");
        assertThat(b.grossTotal()).isEqualByComparingTo("118.00");

        BigDecimal extractedNet = calculator.extractNet(b.grossTotal(), b.vatRate());
        assertThat(extractedNet).isEqualByComparingTo("100.00");
    }

    @Test
    void zeroAndExemptVat_carryNoVatAmount() {
        TotalsBreakdown zero = calculator.calculate(lineItems(1, 100), null, null, VatType.ZERO, RoundingMode.NONE);
        TotalsBreakdown exempt = calculator.calculate(lineItems(1, 100), null, null, VatType.EXEMPT, RoundingMode.NONE);

        assertThat(zero.vatAmount()).isEqualByComparingTo("0.00");
        assertThat(zero.grossTotal()).isEqualByComparingTo("100.00");
        assertThat(exempt.vatAmount()).isEqualByComparingTo("0.00");
        assertThat(exempt.grossTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void roundingToShekel_recordsDelta() {
        TotalsBreakdown b = calculator.calculate(lineItems(1, 100), null, null, VatType.STANDARD, RoundingMode.TO_SHEKEL);

        assertThat(b.preRoundTotal()).isEqualByComparingTo("118.00");
        assertThat(b.grossTotal()).isEqualByComparingTo("118.00");
        assertThat(b.roundingDelta()).isEqualByComparingTo("0.00");
    }

    @Test
    void roundingToAgorot_producesNonZeroDeltaWhenNeeded() {
        // 1 * 33.33 = 33.33 net, VAT 18% => preRound 39.3294 -> rounds to 2dp in intermediate calc already (39.33)
        TotalsBreakdown b = calculator.calculate(lineItems(1, 33.33), null, null, VatType.STANDARD, RoundingMode.TO_AGOROT);

        // 39.33 rounds to nearest 0.10 -> 39.30, delta = 39.33 - 39.30 = 0.03
        assertThat(b.grossTotal()).isEqualByComparingTo("39.30");
        assertThat(b.roundingDelta()).isEqualByComparingTo("0.03");
    }

    @Test
    void noDiscount_whenTypeOrValueMissing() {
        TotalsBreakdown b = calculator.calculate(lineItems(1, 100), null, null, VatType.STANDARD, RoundingMode.NONE);
        assertThat(b.discountAmount()).isEqualByComparingTo("0.00");
    }
}
