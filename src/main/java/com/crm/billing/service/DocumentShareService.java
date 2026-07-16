package com.crm.billing.service;

import com.crm.billing.dto.PaymentRequestResponse;
import com.crm.billing.dto.ShareRequest;
import com.crm.billing.dto.TaxDocumentResponse;
import com.crm.billing.entity.DocumentShareLink;
import com.crm.billing.entity.PaymentRequest;
import com.crm.billing.entity.TaxDocument;
import com.crm.billing.enums.DocumentOwnerType;
import com.crm.service.TranslationService;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Email (attach PDF) / WhatsApp (wa.me link) / print / download — all explicit, never automatic on
 * save. Email attachment follows the same {@code MimeMessageHelper(msg, true, "UTF-8")} +
 * {@code addAttachment} pattern as {@code timetracking.service.ReportEmailService}, since the
 * existing {@code EmailService} has no attachment-capable method to extend.
 *
 * Email subject/body and the WhatsApp message text follow the current user's UI language
 * ({@code TranslationService}, backed by session-bound {@code LocaleService}) — not the recipient's
 * (unknown, possibly not even a CRM user) — since this is the sender's own outbound message.
 */
@Service
public class DocumentShareService {

    private final JavaMailSender mailSender;
    private final DocumentPdfService pdfService;
    private final MessageChannelSender messageChannelSender;
    private final PaymentRequestService paymentRequestService;
    private final TaxDocumentDraftService taxDocumentDraftService;
    private final DocumentShareLinkService shareLinkService;
    private final TranslationService i18n;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.base-url:http://localhost:9080}")
    private String baseUrl;

    public DocumentShareService(JavaMailSender mailSender,
                                 DocumentPdfService pdfService,
                                 MessageChannelSender messageChannelSender,
                                 PaymentRequestService paymentRequestService,
                                 TaxDocumentDraftService taxDocumentDraftService,
                                 DocumentShareLinkService shareLinkService,
                                 TranslationService i18n) {
        this.mailSender = mailSender;
        this.pdfService = pdfService;
        this.messageChannelSender = messageChannelSender;
        this.paymentRequestService = paymentRequestService;
        this.taxDocumentDraftService = taxDocumentDraftService;
        this.shareLinkService = shareLinkService;
        this.i18n = i18n;
    }

    /** Used by both the "print" (stream inline) and "download" (save-as) share actions — the bytes
     * are identical, only the controller's response headers differ. */
    public byte[] renderPdf(DocumentOwnerType ownerType, Long documentId) {
        return switch (ownerType) {
            case PAYMENT_REQUEST -> {
                PaymentRequest pr = paymentRequestService.getOwned(documentId);
                PaymentRequestResponse response = paymentRequestService.findById(documentId);
                yield pdfService.renderPaymentRequest(response, pr.getWorkspace());
            }
            case TAX_DOCUMENT -> {
                TaxDocument doc = taxDocumentDraftService.getOwned(documentId);
                TaxDocumentResponse response = taxDocumentDraftService.findById(documentId);
                yield pdfService.renderTaxDocument(response, doc.getWorkspace());
            }
        };
    }

    /** Serves a PDF for the public, unauthenticated share-link download path — the token has already
     * been resolved (and its expiry checked) by {@link DocumentShareLinkService} before this is
     * called, so tenant scoping is deliberately bypassed here (there is no authenticated caller). */
    public byte[] renderPdfForShareLink(DocumentShareLink link) {
        return switch (link.getOwnerType()) {
            case PAYMENT_REQUEST -> {
                PaymentRequest pr = paymentRequestService.getTrusted(link.getOwnerId());
                yield pdfService.renderPaymentRequest(paymentRequestService.toResponseForShareLink(pr), pr.getWorkspace());
            }
            case TAX_DOCUMENT -> {
                TaxDocument doc = taxDocumentDraftService.getTrusted(link.getOwnerId());
                yield pdfService.renderTaxDocument(taxDocumentDraftService.toResponseForShareLink(doc), doc.getWorkspace());
            }
        };
    }

    public void share(ShareRequest request) throws jakarta.mail.MessagingException {
        if (request.channel() == ShareRequest.ShareChannel.EMAIL) {
            shareByEmail(request.ownerType(), request.documentId(), request.target());
        }
        // WHATSAPP has no server-side send step — DocumentController builds the deep link directly
        // via buildWhatsAppLink() and returns it to the caller to open.
    }

    /** wa.me/api.whatsapp.com links can only pre-fill text — there is no attachment parameter, only
     * the paid WhatsApp Business Cloud API can send a document programmatically. Instead, this
     * embeds a public, expiring, single-document download link (see {@link DocumentShareLinkService})
     * in the pre-filled message, so the recipient (not a CRM user) can tap it to get the PDF
     * directly without the sender manually attaching anything inside WhatsApp. */
    public String buildWhatsAppLink(DocumentOwnerType ownerType, Long documentId, String phoneNumber) {
        String label = documentLabel(ownerType, documentId);
        String token = shareLinkService.createLink(ownerType, documentId);
        String downloadUrl = baseUrl + "/api/v1/billing/public/" + token + "/pdf";
        String message = label + "\n" + i18n.translate("email.billingShare.downloadLink") + " " + downloadUrl;
        return messageChannelSender.buildDeepLink(phoneNumber, message);
    }

    private void shareByEmail(DocumentOwnerType ownerType, Long documentId, String toEmail) throws jakarta.mail.MessagingException {
        byte[] pdf = renderPdf(ownerType, documentId);
        String label = documentLabel(ownerType, documentId);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromAddress);
        helper.setTo(toEmail);
        helper.setSubject(i18n.translate("email.billingShare.subject", label));
        helper.setText(i18n.translate("email.billingShare.greeting") + "\n\n"
                + i18n.translate("email.billingShare.body", label));
        helper.addAttachment(label.replaceAll("[^\\w\\-]", "_") + ".pdf", new ByteArrayResource(pdf), "application/pdf");
        mailSender.send(message);
    }

    private String documentLabel(DocumentOwnerType ownerType, Long documentId) {
        return switch (ownerType) {
            case PAYMENT_REQUEST -> {
                PaymentRequestResponse pr = paymentRequestService.findById(documentId);
                yield i18n.translate("email.billingShare.paymentRequest") + " " + pr.number()
                        + " - " + pr.currency() + " " + pr.totalAmount();
            }
            case TAX_DOCUMENT -> {
                TaxDocumentResponse doc = taxDocumentDraftService.findById(documentId);
                yield i18n.translateEnum(doc.documentType()) + " " + doc.number()
                        + " - " + doc.currency() + " " + doc.grossTotal();
            }
        };
    }
}
