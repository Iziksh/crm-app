package com.crm.billing.service;

import com.crm.billing.dto.TaxDocumentCreateRequest;
import com.crm.billing.dto.TaxDocumentResponse;
import com.crm.billing.entity.DocumentLineItem;
import com.crm.billing.entity.PaymentRequest;
import com.crm.billing.entity.TaxDocument;
import com.crm.billing.enums.DocumentOwnerType;
import com.crm.billing.enums.DocumentStatus;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.enums.PaymentRequestStatus;
import com.crm.billing.enums.RoundingMode;
import com.crm.billing.enums.VatType;
import com.crm.billing.exception.IllegalConversionTargetForTaxStatusException;
import com.crm.billing.exception.NotEditableException;
import com.crm.billing.repository.TaxDocumentRepository;
import com.crm.domain.entity.Workspace;
import com.crm.domain.enums.WorkspaceTaxStatus;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Payment request -> tax document. Idempotent and resumable at every step, and gated by
 * {@code Workspace.taxStatus}.
 *
 * Deliberately NOT itself {@code @Transactional}: each step below (draft creation, issuing,
 * marking the payment request converted) is already transactional in its own service, and each is
 * individually safe to retry. Wrapping all of them in one longer transaction here would force
 * {@code TaxDocumentIssueService}'s nested allocation-failure handling (which relies on a
 * REQUIRES_NEW sub-transaction seeing an already-committed TaxDocument row) to run against a row
 * this same transaction hasn't committed yet — invisible to it under Postgres's read-committed
 * isolation. Splitting into independently-committed steps avoids that and gets resumability for free.
 */
@Service
public class ConversionService {

    private final PaymentRequestService paymentRequestService;
    private final TaxDocumentRepository taxDocumentRepository;
    private final TaxDocumentDraftService draftService;
    private final TaxDocumentIssueService issueService;
    private final DocumentLineItemSupport lineItemSupport;

    public ConversionService(PaymentRequestService paymentRequestService,
                              TaxDocumentRepository taxDocumentRepository,
                              TaxDocumentDraftService draftService,
                              TaxDocumentIssueService issueService,
                              DocumentLineItemSupport lineItemSupport) {
        this.paymentRequestService = paymentRequestService;
        this.taxDocumentRepository = taxDocumentRepository;
        this.draftService = draftService;
        this.issueService = issueService;
        this.lineItemSupport = lineItemSupport;
    }

    public TaxDocumentResponse convert(Long paymentRequestId, DocumentType targetDocumentType) {
        PaymentRequest pr = paymentRequestService.getOwned(paymentRequestId);

        if (pr.getConvertedDocumentId() != null) {
            return existingConvertedResponse(pr.getConvertedDocumentId());
        }

        Optional<TaxDocument> existing = taxDocumentRepository.findBySourcePaymentRequestId(pr.getId());
        if (existing.isPresent() && existing.get().getStatus() == DocumentStatus.ISSUED) {
            // Issued in a previous attempt, but the payment request update didn't land — finish it.
            paymentRequestService.markConverted(pr.getId(), existing.get().getId());
            return existingConvertedResponse(existing.get().getId());
        }
        if (pr.getStatus() == PaymentRequestStatus.CANCELLED) {
            throw new NotEditableException("Cannot convert a cancelled payment request");
        }

        TaxDocument draft = existing.orElseGet(() -> createDraft(pr, targetDocumentType));

        issueService.issue(draft.getId());
        TaxDocument issued = taxDocumentRepository.findByIdFetched(draft.getId()).orElseThrow();

        paymentRequestService.markConverted(pr.getId(), issued.getId());

        return draftService.toResponse(issued, lineItemSupport.find(DocumentOwnerType.TAX_DOCUMENT, issued.getId()));
    }

    private TaxDocument createDraft(PaymentRequest pr, DocumentType targetDocumentType) {
        Workspace workspace = pr.getWorkspace();
        WorkspaceTaxStatus taxStatus = workspace.getTaxStatus();
        if (taxStatus == null) {
            throw new IllegalConversionTargetForTaxStatusException(
                    "Workspace has no billing tax status configured — set Workspace.taxStatus before converting");
        }
        validateTarget(taxStatus, targetDocumentType);

        List<DocumentLineItem> items = lineItemSupport.find(DocumentOwnerType.PAYMENT_REQUEST, pr.getId());
        VatType defaultVatType = taxStatus == WorkspaceTaxStatus.EXEMPT_DEALER ? VatType.EXEMPT : VatType.STANDARD;
        TaxDocumentCreateRequest draftRequest = new TaxDocumentCreateRequest(
                targetDocumentType, pr.getAccount().getId(), pr.getCurrency(), pr.getDocumentDate(),
                pr.getFreeText(), defaultVatType, null, null, RoundingMode.NONE,
                lineItemSupport.toRequests(items));

        TaxDocument draft = draftService.createEntity(workspace, draftRequest);
        draft.setSourcePaymentRequestId(pr.getId());
        return draftService.saveLinkedDraft(draft);
    }

    private TaxDocumentResponse existingConvertedResponse(Long taxDocumentId) {
        TaxDocument doc = taxDocumentRepository.findByIdFetched(taxDocumentId).orElseThrow();
        return draftService.toResponse(doc, lineItemSupport.find(DocumentOwnerType.TAX_DOCUMENT, doc.getId()));
    }

    private void validateTarget(WorkspaceTaxStatus taxStatus, DocumentType target) {
        Set<DocumentType> allowed = switch (taxStatus) {
            case EXEMPT_DEALER -> EnumSet.of(DocumentType.RECEIPT);
            case AUTHORIZED_DEALER, LIMITED_COMPANY -> EnumSet.of(DocumentType.TAX_INVOICE_RECEIPT, DocumentType.PROFORMA);
        };
        if (!allowed.contains(target)) {
            throw new IllegalConversionTargetForTaxStatusException(
                    "Workspace tax status " + taxStatus + " cannot convert a payment request into " + target);
        }
    }
}
