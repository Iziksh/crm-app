package com.crm.billing.service;

import com.crm.billing.dto.LineItemResponse;
import com.crm.billing.dto.PaymentRequestResponse;
import com.crm.billing.dto.TaxDocumentResponse;
import com.crm.billing.enums.AllocationStatus;
import com.crm.billing.enums.DiscountType;
import com.crm.billing.enums.DocumentStatus;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.enums.PaymentRequestStatus;
import com.crm.billing.enums.RoundingMode;
import com.crm.billing.enums.VatType;
import com.crm.domain.entity.Workspace;
import com.crm.domain.enums.WorkspaceTaxStatus;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentPdfServiceTest {

    private final DocumentPdfService pdfService = new DocumentPdfService();

    private Workspace workspace(WorkspaceTaxStatus taxStatus) {
        Workspace ws = new Workspace();
        ws.setId(1L);
        ws.setName("Acme");
        ws.setTaxStatus(taxStatus);
        ws.setTaxId("123456789");
        return ws;
    }

    private List<LineItemResponse> lineItems() {
        return List.of(new LineItemResponse(1L, "Widget", "desc", BigDecimal.ONE, BigDecimal.TEN, BigDecimal.TEN, 0));
    }

    private String extractText(byte[] pdfBytes) throws Exception {
        try (PdfDocument doc = new PdfDocument(new PdfReader(new java.io.ByteArrayInputStream(pdfBytes)))) {
            return PdfTextExtractor.getTextFromPage(doc.getPage(1));
        }
    }

    @Test
    void paymentRequestPdf_rendersValidPdf_withSignature() throws Exception {
        PaymentRequestResponse pr = new PaymentRequestResponse(1L, "1001", 2026, PaymentRequestStatus.OPEN,
                "ILS", LocalDate.of(2026, 7, 1), "note", BigDecimal.TEN, 2L, "Customer Ltd", "123456789",
                null, lineItems(), null, null);

        byte[] pdf = pdfService.renderPaymentRequest(pr, workspace(WorkspaceTaxStatus.AUTHORIZED_DEALER));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        String text = extractText(pdf);
        assertThat(text).isNotBlank();
    }

    @Test
    void taxDocumentPdf_vatLine_presentForAuthorized_absentForExempt() throws Exception {
        TaxDocumentResponse authorized = taxDocumentResponse(VatType.STANDARD, "ALLOCNUMBER1");
        TaxDocumentResponse exempt = taxDocumentResponse(VatType.EXEMPT, null);

        byte[] authorizedPdf = pdfService.renderTaxDocument(authorized, workspace(WorkspaceTaxStatus.AUTHORIZED_DEALER));
        byte[] exemptPdf = pdfService.renderTaxDocument(exempt, workspace(WorkspaceTaxStatus.EXEMPT_DEALER));

        assertThat(authorizedPdf).isNotEmpty();
        assertThat(exemptPdf).isNotEmpty();

        String authorizedText = extractText(authorizedPdf);
        String exemptText = extractText(exemptPdf);

        // VAT breakdown adds two extra lines (net + VAT amount) plus the allocation-number line,
        // only in the authorized/STANDARD-VAT case.
        assertThat(authorizedText.length()).isGreaterThan(exemptText.length());
        // Allocation number is routed to the Latin (Helvetica) font as a pure-LTR BiDi run, so it
        // must extract intact — this used to fail before the BiDi-run/font-fallback fix.
        assertThat(authorizedText).contains("ALLOCNUMBER1");
        assertThat(exemptText).doesNotContain("ALLOCNUMBER1");
    }

    @Test
    void mixedHebrewAndPunctuation_allCharactersRender_noneDropped() throws Exception {
        // Regression test for the "missing glyph" bug: neutral punctuation (":", "/", quote marks,
        // "_") adjacent to Hebrew text resolves to the *same* BiDi level as that Hebrew text, so
        // routing fonts per BiDi-run (rather than per character) sent this punctuation to the
        // Hebrew-only font, which doesn't have those glyphs — it dropped silently (empty text, no
        // exception) rather than rendering as a box, since iText simply skips undisplayable glyphs
        // in a font that lacks them entirely. Text extraction would show the character was never
        // written to the content stream at all.
        PaymentRequestResponse pr = new PaymentRequestResponse(1L, "1001", 2026, PaymentRequestStatus.OPEN,
                "ILS", LocalDate.of(2026, 7, 1), null, BigDecimal.TEN, 2L, "Customer Ltd", null,
                null, lineItems(), null, null);

        byte[] pdf = pdfService.renderPaymentRequest(pr, workspace(WorkspaceTaxStatus.AUTHORIZED_DEALER));
        String text = extractText(pdf);

        assertThat(text).contains(":"); // תאריך: / סטטוס: colons
        assertThat(text).contains("/"); // מוצר/שירות slash
        assertThat(text).contains("׳"); // מס׳ geresh
        assertThat(text).contains("_"); // signature line
    }

    @Test
    void hebrewText_visuallyReorderedForCorrectDisplay_digitsUnaffected() throws Exception {
        // DocumentPdfService pre-reverses RTL runs itself (see class javadoc: iText's free core
        // doesn't do this without the paid pdfCalligraph module) so the PDF *displays* correctly
        // right-to-left. That pre-reversal is baked into the content stream, so text *extraction*
        // of a pure-Hebrew run necessarily comes back reversed too ("לחם" extracts as "םחל") — an
        // accepted trade-off for correct on-screen/print rendering without pdfCalligraph, not a bug.
        // Digits are never reversed (they get their own separate BiDi run) and must extract intact.
        List<LineItemResponse> hebrewItems = List.of(
                new LineItemResponse(1L, "לחם", null, BigDecimal.ONE, new BigDecimal("12.50"),
                        new BigDecimal("12.50"), 0)); // "לחם" (Bread)
        PaymentRequestResponse pr = new PaymentRequestResponse(1L, "1001", 2026, PaymentRequestStatus.OPEN,
                "ILS", LocalDate.of(2026, 7, 1), null, new BigDecimal("12.50"), 2L, "Customer Ltd", null,
                null, hebrewItems, null, null);

        byte[] pdf = pdfService.renderPaymentRequest(pr, workspace(WorkspaceTaxStatus.AUTHORIZED_DEALER));
        String text = extractText(pdf);

        assertThat(text).contains("םחל"); // "לחם" reversed — expected extraction of a pure-RTL run
        assertThat(text).contains("12.50"); // decimal price, routed to the Latin font, extracts intact
    }

    private TaxDocumentResponse taxDocumentResponse(VatType vatType, String allocationNumber) {
        return new TaxDocumentResponse(1L, DocumentType.TAX_INVOICE_RECEIPT, "INV-2026-00001", 2026,
                DocumentStatus.ISSUED, "ILS", LocalDate.of(2026, 7, 1), "note", vatType,
                DiscountType.PERCENT, BigDecimal.ZERO, RoundingMode.NONE,
                BigDecimal.TEN, vatType == VatType.STANDARD ? new BigDecimal("0.18") : BigDecimal.ZERO,
                vatType == VatType.STANDARD ? new BigDecimal("1.80") : BigDecimal.ZERO,
                BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN,
                allocationNumber != null ? AllocationStatus.ISSUED : AllocationStatus.NOT_REQUIRED,
                allocationNumber, null, null, null, 2L, "Customer Ltd", "123456789", lineItems(), List.of());
    }
}
