package com.crm.billing.service;

import com.crm.billing.dto.LineItemResponse;
import com.crm.billing.dto.PaymentRequestResponse;
import com.crm.billing.dto.TaxDocumentResponse;
import com.crm.billing.enums.VatType;
import com.crm.domain.entity.Workspace;
import com.crm.domain.enums.WorkspaceTaxStatus;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.text.Bidi;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * RTL/Hebrew PDF renderer, built fresh with iText7 (kernel + layout) since no PDF stack existed in
 * this codebase before Phase 26. Hebrew glyphs come from a bundled Noto Sans Hebrew TTF
 * (src/main/resources/fonts).
 *
 * THREE non-obvious iText/font/BiDi issues were found and fixed here — worth reading before
 * touching this class again:
 *
 * 1. iText's free core does NOT reorder RTL text without the paid pdfCalligraph module —
 *    {@code Paragraph.setBaseDirection(RIGHT_TO_LEFT)} is silently a no-op without it (visible at
 *    runtime as a "Cannot find pdfCalligraph module" warning), so Hebrew rendered visually
 *    mirrored/reversed. Fixed by running Java's own Unicode BiDi implementation
 *    ({@link java.text.Bidi}) ourselves: split each line into runs, reverse character order within
 *    RTL (odd-level) runs, and place all runs via {@link Bidi#reorderVisually} — then feed iText the
 *    already visually-ordered text and never ask it to do RTL reordering itself.
 * 2. Noto Sans Hebrew (a script-specific Google Font) ships Hebrew glyphs only — no digits, Latin
 *    letters, or ASCII punctuation (confirmed via {@code Font.canDisplay}), so those rendered as
 *    empty "tofu" boxes.
 * 3. Selecting a font per *BiDi run* (not per character) is insufficient: neutral characters
 *    (":", "/", "'", "\"", "_") adjacent to Hebrew text resolve to the *same* BiDi level as that
 *    Hebrew text (per the Unicode BiDi algorithm's neutral-resolution rules), so a whole run like
 *    "מוצר/שירות" or "תאריך:" is one single RTL-level run even though it contains characters the
 *    Hebrew font can't display. Fixed by a second, finer pass: after computing visual run order,
 *    each run is further split into contiguous same-script segments by literal Unicode block
 *    (Hebrew block U+0590–U+05FF vs everything else), and each segment gets whichever font actually
 *    covers it — the bundled Hebrew font for Hebrew-block characters, iText's built-in standard
 *    Helvetica (always available, no embedding needed) for everything else. Hebrew abbreviation
 *    marks use the real Hebrew geresh/gershayim (U+05F3/U+05F4, e.g. "מס׳", "סה״כ"), which the
 *    Hebrew font does cover, rather than ASCII quote characters, which it doesn't.
 *
 * KNOWN TRADE-OFF: the manual reversal in point 1 fixes on-screen/print *display* (what the prompt
 * this class was built for actually cares about) but the fix is baked into the content stream, so
 * copy/paste or programmatic text *extraction* of a pure-Hebrew run comes back character-reversed
 * (e.g. "לחם" extracts as "םחל") — there is no ToUnicode/ActualText mapping back to logical order.
 * Digits and Latin text are unaffected (never reversed). If extraction/accessibility fidelity
 * becomes a requirement, add PDF `ActualText` marked content per run (or pdfCalligraph) rather than
 * assuming this reversal-based approach can be made extraction-safe as-is.
 */
@Service
public class DocumentPdfService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final byte[] hebrewRegularBytes;
    private final byte[] hebrewBoldBytes;

    public DocumentPdfService() {
        this.hebrewRegularBytes = loadFontBytes("/fonts/NotoSansHebrew-Regular.ttf");
        this.hebrewBoldBytes = loadFontBytes("/fonts/NotoSansHebrew-Bold.ttf");
    }

    private static byte[] loadFontBytes(String classpathResource) {
        try (InputStream in = DocumentPdfService.class.getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IOException("Font resource not found on classpath: " + classpathResource);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load Hebrew PDF font " + classpathResource, e);
        }
    }

    private static PdfFont newEmbeddedFont(byte[] fontBytes) {
        try {
            return PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, EmbeddingStrategy.FORCE_EMBEDDED);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to instantiate Hebrew PDF font", e);
        }
    }

    private static PdfFont newStandardFont(String standardFontName) {
        try {
            return PdfFontFactory.createFont(standardFontName);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to instantiate standard PDF font " + standardFontName, e);
        }
    }

    /** Per-render font set — never shared across {@code build()} calls or threads (iText binds a
     * font's indirect object to whichever {@code PdfDocument} first embeds it). */
    private record Fonts(PdfFont hebrewRegular, PdfFont hebrewBold, PdfFont latinRegular, PdfFont latinBold) {}

    public byte[] renderPaymentRequest(PaymentRequestResponse pr, Workspace workspace) {
        String statusLabel = switch (pr.status()) {
            case OPEN -> "מסמך פתוח";
            case CONVERTED -> "הומר למסמך חשבונאי";
            case CANCELLED -> "מבוטל";
        };
        return build(workspace, "דרישת תשלום", pr.number(), pr.documentDate(), statusLabel,
                pr.accountName(), pr.accountTaxId(), pr.currency(), pr.lineItems(),
                pr.totalAmount(), false, null, null, null, null);
    }

    public byte[] renderTaxDocument(TaxDocumentResponse doc, Workspace workspace) {
        boolean showVat = doc.vatType() == VatType.STANDARD;
        String title = switch (doc.documentType()) {
            case TAX_INVOICE -> "חשבונית מס";
            case TAX_INVOICE_RECEIPT -> "חשבונית מס/קבלה";
            case RECEIPT -> "קבלה";
            case PROFORMA -> "חשבון עסקה";
            case QUOTE -> "הצעת מחיר";
            case CREDIT_INVOICE -> "חשבונית זיכוי";
        };
        return build(workspace, title, doc.number(), doc.documentDate(), doc.status().name(),
                doc.accountName(), doc.accountTaxId(), doc.currency(), doc.lineItems(),
                doc.grossTotal(), showVat, doc.vatRate(), doc.vatAmount(), doc.netTotal(),
                doc.allocationNumber());
    }

    private byte[] build(Workspace workspace, String title, String number, LocalDate documentDate,
                          String statusLabel, String customerName, String customerTaxId, String currency,
                          List<LineItemResponse> lineItems, BigDecimal totalToShow, boolean showVat,
                          BigDecimal vatRate, BigDecimal vatAmount, BigDecimal netTotal, String allocationNumber) {
        Fonts fonts = new Fonts(newEmbeddedFont(hebrewRegularBytes), newEmbeddedFont(hebrewBoldBytes),
                newStandardFont(StandardFonts.HELVETICA), newStandardFont(StandardFonts.HELVETICA_BOLD));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc, PageSize.A4)) {

            document.setFont(fonts.hebrewRegular());
            document.setTextAlignment(TextAlignment.RIGHT);

            addBusinessHeader(document, workspace, fonts);
            addTitleAndNumber(document, title, number, documentDate, statusLabel, fonts);
            addCustomerLine(document, customerName, customerTaxId, fonts);
            addLineItemsTable(document, lineItems, currency, fonts);
            addTotals(document, currency, totalToShow, showVat, vatRate, vatAmount, netTotal, fonts);
            if (allocationNumber != null) {
                addAllocationNumber(document, allocationNumber, fonts);
            }
            addSignatureAndFooter(document, fonts);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render PDF", e);
        }
        return baos.toByteArray();
    }

    // ── BiDi-aware text building ────────────────────────────────────────────────────────────────

    /** A contiguous same-script segment, already in final visual (left-to-right drawing) order. */
    private record Segment(String text, boolean hebrew) {}

    private static boolean isHebrewChar(char c) {
        return c >= 0x0590 && c <= 0x05FF;
    }

    /** Splits already visually-ordered text into contiguous Hebrew-block vs. everything-else
     * segments, so each can be handed to whichever font actually covers it. Spaces stay attached to
     * whichever segment is already open, since both fonts cover plain space. */
    private static List<Segment> splitByScript(String visualText) {
        List<Segment> segments = new java.util.ArrayList<>();
        if (visualText.isEmpty()) {
            return segments;
        }
        StringBuilder current = new StringBuilder();
        boolean currentHebrew = isHebrewChar(visualText.charAt(0));
        for (int i = 0; i < visualText.length(); i++) {
            char c = visualText.charAt(i);
            boolean hebrew = isHebrewChar(c);
            if (hebrew == currentHebrew || c == ' ') {
                current.append(c);
            } else {
                segments.add(new Segment(current.toString(), currentHebrew));
                current = new StringBuilder().append(c);
                currentHebrew = hebrew;
            }
        }
        segments.add(new Segment(current.toString(), currentHebrew));
        return segments;
    }

    /** Builds a right-aligned Paragraph from logical (natural reading order) text that may mix
     * Hebrew and Latin/digits/punctuation. Two passes: (1) Java's Unicode BiDi implementation
     * computes correct visual run order and reverses character order within RTL runs; (2) each
     * visually-ordered run is further split by literal Unicode block so punctuation the Hebrew font
     * can't display still renders, via Helvetica — see class javadoc for why both passes are needed. */
    private Paragraph bidiParagraph(String logical, PdfFont hebrewFont, PdfFont latinFont, float fontSize) {
        Paragraph p = new Paragraph().setTextAlignment(TextAlignment.RIGHT).setMultipliedLeading(1.2f);
        if (logical == null || logical.isEmpty()) {
            return p;
        }
        Bidi bidi = new Bidi(logical, Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
        int runCount = bidi.getRunCount();
        byte[] levels = new byte[runCount];
        Integer[] order = new Integer[runCount];
        List<List<Segment>> segmentsByRun = new java.util.ArrayList<>(runCount);
        for (int i = 0; i < runCount; i++) {
            levels[i] = (byte) bidi.getRunLevel(i);
            order[i] = i;
            boolean rtl = levels[i] % 2 == 1;
            String runText = logical.substring(bidi.getRunStart(i), bidi.getRunLimit(i));
            String visualRunText = rtl ? new StringBuilder(runText).reverse().toString() : runText;
            segmentsByRun.add(splitByScript(visualRunText));
        }
        Bidi.reorderVisually(levels, 0, order, 0, runCount);
        for (int runIndex : order) {
            for (Segment segment : segmentsByRun.get(runIndex)) {
                Text text = new Text(segment.text()).setFont(segment.hebrew() ? hebrewFont : latinFont).setFontSize(fontSize);
                p.add(text);
            }
        }
        return p;
    }

    private Paragraph regular(String logical, Fonts fonts, float fontSize) {
        return bidiParagraph(logical, fonts.hebrewRegular(), fonts.latinRegular(), fontSize);
    }

    private Paragraph bold(String logical, Fonts fonts, float fontSize) {
        return bidiParagraph(logical, fonts.hebrewBold(), fonts.latinBold(), fontSize);
    }

    // ── Layout ───────────────────────────────────────────────────────────────────────────────────

    private void addBusinessHeader(Document document, Workspace workspace, Fonts fonts) {
        String name = workspace.getLegalName() != null ? workspace.getLegalName() : workspace.getName();
        document.add(bold(name, fonts, 16));

        String taxStatusLabel = taxStatusLabel(workspace.getTaxStatus());
        StringBuilder line2 = new StringBuilder();
        if (taxStatusLabel != null) line2.append(taxStatusLabel);
        if (workspace.getTaxId() != null) {
            if (line2.length() > 0) line2.append(" | ");
            line2.append("ע.מ/ח.פ ").append(workspace.getTaxId());
        }
        if (line2.length() > 0) {
            document.add(regular(line2.toString(), fonts, 10));
        }
        StringBuilder line3 = new StringBuilder();
        appendIfPresent(line3, workspace.getBusinessAddress());
        appendIfPresent(line3, workspace.getBusinessPhone());
        appendIfPresent(line3, workspace.getBusinessEmail());
        if (line3.length() > 0) {
            document.add(regular(line3.toString(), fonts, 10));
        }
        document.add(new LineSeparator(new SolidLine(0.5f)));
    }

    private void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(value);
        }
    }

    private String taxStatusLabel(WorkspaceTaxStatus taxStatus) {
        if (taxStatus == null) return null;
        return switch (taxStatus) {
            case EXEMPT_DEALER -> "עוסק פטור";
            case AUTHORIZED_DEALER -> "עוסק מורשה";
            case LIMITED_COMPANY -> "חברה בע״מ";
        };
    }

    private void addTitleAndNumber(Document document, String title, String number, LocalDate documentDate,
                                    String statusLabel, Fonts fonts) {
        String numberPart = number != null ? " מס׳ " + number : "";
        document.add(bold(title + numberPart, fonts, 14));
        String dateStr = documentDate != null ? documentDate.format(DATE_FORMAT) : "";
        document.add(regular("תאריך: " + dateStr + (statusLabel != null ? "   |   סטטוס: " + statusLabel : ""), fonts, 10));
    }

    private void addCustomerLine(Document document, String customerName, String customerTaxId, Fonts fonts) {
        String line = "לכבוד: " + (customerName != null ? customerName : "");
        if (customerTaxId != null && !customerTaxId.isBlank()) {
            line += "   ת״ז/ח״פ: " + customerTaxId;
        }
        document.add(bold(line, fonts, 12));
    }

    private void addLineItemsTable(Document document, List<LineItemResponse> lineItems, String currency, Fonts fonts) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{4, 1, 1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        table.addHeaderCell(headerCell("מוצר/שירות", fonts));
        table.addHeaderCell(headerCell("כמות", fonts));
        table.addHeaderCell(headerCell("מחיר", fonts));
        table.addHeaderCell(headerCell("סה״כ", fonts));

        for (LineItemResponse item : lineItems) {
            Cell productCell = new Cell().setTextAlignment(TextAlignment.CENTER).setBorder(new SolidBorder(0.5f));
            productCell.add(regular(item.productOrService(), fonts, 10));
            if (item.description() != null && !item.description().isBlank()) {
                for (String line : item.description().split("\n")) {
                    productCell.add(regular(line, fonts, 9));
                }
            }
            table.addCell(productCell);
            table.addCell(bodyCell(String.valueOf(item.quantity()), fonts));
            table.addCell(bodyCell(currency + " " + item.unitPrice(), fonts));
            table.addCell(bodyCell(currency + " " + item.lineTotal(), fonts));
        }
        document.add(table);
    }

    private Cell headerCell(String text, Fonts fonts) {
        return new Cell().add(bold(text, fonts, 10))
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(new SolidBorder(0.5f));
    }

    private Cell bodyCell(String text, Fonts fonts) {
        return new Cell().add(regular(text, fonts, 10))
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(new SolidBorder(0.5f));
    }

    private void addTotals(Document document, String currency, BigDecimal totalToShow, boolean showVat,
                            BigDecimal vatRate, BigDecimal vatAmount, BigDecimal netTotal, Fonts fonts) {
        if (showVat && netTotal != null) {
            document.add(regular("סכום לפני מע״מ: " + currency + " " + netTotal, fonts, 11));
            String ratePct = vatRate != null ? vatRate.movePointRight(2) + "%" : "";
            document.add(regular("מע״מ (" + ratePct + "): " + currency + " " + vatAmount, fonts, 11));
        }
        document.add(bold("סה״כ לתשלום: " + currency + " " + totalToShow, fonts, 13));
    }

    private void addAllocationNumber(Document document, String allocationNumber, Fonts fonts) {
        document.add(regular("מספר הקצאה: " + allocationNumber, fonts, 11));
    }

    private void addSignatureAndFooter(Document document, Fonts fonts) {
        document.add(new Paragraph("\n"));
        document.add(regular("_________________________", fonts, 10));
        document.add(regular("חתימה", fonts, 9));
        document.add(regular("עובד/ת", fonts, 8)
                .setTextAlignment(TextAlignment.CENTER)
                .setHorizontalAlignment(HorizontalAlignment.CENTER));
    }
}
