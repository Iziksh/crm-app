package com.crm.billing.service;

import com.crm.billing.dto.DocumentPaymentRequest;
import com.crm.billing.dto.DocumentPaymentResponse;
import com.crm.billing.dto.LineItemResponse;
import com.crm.billing.dto.TaxDocumentCreateRequest;
import com.crm.billing.dto.TaxDocumentResponse;
import com.crm.billing.dto.TotalsBreakdown;
import com.crm.billing.entity.DocumentLineItem;
import com.crm.billing.entity.DocumentPayment;
import com.crm.billing.entity.TaxDocument;
import com.crm.billing.enums.DocumentOwnerType;
import com.crm.billing.enums.DocumentStatus;
import com.crm.billing.enums.RoundingMode;
import com.crm.billing.enums.VatType;
import com.crm.billing.exception.CrossTenantAccessException;
import com.crm.billing.exception.ImmutableDocumentException;
import com.crm.billing.exception.NotEditableException;
import com.crm.billing.repository.DocumentPaymentRepository;
import com.crm.billing.repository.TaxDocumentRepository;
import com.crm.domain.entity.Account;
import com.crm.domain.entity.Workspace;
import com.crm.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** DRAFT-only editing of tax documents (חשבונית מס/קבלה etc). Numbering/allocation/immutability
 * happen only in {@link TaxDocumentIssueService}. */
@Service
@Transactional
public class TaxDocumentDraftService {

    private final TaxDocumentRepository taxDocumentRepository;
    private final DocumentPaymentRepository paymentRepository;
    private final DocumentLineItemSupport lineItemSupport;
    private final BillingTenantSupport tenantSupport;
    private final TotalsCalculator totalsCalculator;

    public TaxDocumentDraftService(TaxDocumentRepository taxDocumentRepository,
                                    DocumentPaymentRepository paymentRepository,
                                    DocumentLineItemSupport lineItemSupport,
                                    BillingTenantSupport tenantSupport,
                                    TotalsCalculator totalsCalculator) {
        this.taxDocumentRepository = taxDocumentRepository;
        this.paymentRepository = paymentRepository;
        this.lineItemSupport = lineItemSupport;
        this.tenantSupport = tenantSupport;
        this.totalsCalculator = totalsCalculator;
    }

    public TaxDocumentResponse create(TaxDocumentCreateRequest request) {
        Workspace workspace = tenantSupport.currentWorkspaceOrThrow();
        TaxDocument doc = createEntity(workspace, request);
        return toResponse(doc, lineItemSupport.find(DocumentOwnerType.TAX_DOCUMENT, doc.getId()));
    }

    /** Used by {@link ConversionService}, which already knows the workspace (from the source
     * payment request) and needs the raw entity to stamp {@code sourcePaymentRequestId} before
     * issuing it. */
    TaxDocument createEntity(Workspace workspace, TaxDocumentCreateRequest request) {
        Account account = tenantSupport.loadCustomer(request.accountId(), workspace);

        TaxDocument doc = new TaxDocument();
        doc.setWorkspace(workspace);
        doc.setAccount(account);
        doc.setDocumentType(request.documentType());
        doc.setCurrency(request.currency() != null ? request.currency() : "ILS");
        LocalDate date = request.documentDate() != null ? request.documentDate() : LocalDate.now();
        doc.setDocumentDate(date);
        doc.setDocumentYear(date.getYear());
        doc.setFreeText(request.freeText());
        doc.setVatType(request.vatType() != null ? request.vatType() : VatType.STANDARD);
        doc.setDiscountType(request.discountType());
        doc.setDiscountValue(request.discountValue() != null ? request.discountValue() : BigDecimal.ZERO);
        doc.setRoundingMode(request.roundingMode() != null ? request.roundingMode() : RoundingMode.NONE);
        doc.setStatus(DocumentStatus.DRAFT);
        doc = taxDocumentRepository.save(doc);

        List<DocumentLineItem> items = lineItemSupport.replaceItems(
                DocumentOwnerType.TAX_DOCUMENT, doc.getId(), request.lineItems());
        recalcTotals(doc, items);
        doc = taxDocumentRepository.save(doc);

        return doc;
    }

    public TaxDocumentResponse update(Long id, TaxDocumentCreateRequest request) {
        TaxDocument doc = getOwned(id);
        requireDraft(doc);

        Account account = tenantSupport.loadCustomer(request.accountId(), doc.getWorkspace());
        doc.setAccount(account);
        doc.setDocumentType(request.documentType());
        if (request.currency() != null) doc.setCurrency(request.currency());
        if (request.documentDate() != null) {
            doc.setDocumentDate(request.documentDate());
            doc.setDocumentYear(request.documentDate().getYear());
        }
        doc.setFreeText(request.freeText());
        doc.setVatType(request.vatType() != null ? request.vatType() : VatType.STANDARD);
        doc.setDiscountType(request.discountType());
        doc.setDiscountValue(request.discountValue() != null ? request.discountValue() : BigDecimal.ZERO);
        doc.setRoundingMode(request.roundingMode() != null ? request.roundingMode() : RoundingMode.NONE);

        List<DocumentLineItem> items = lineItemSupport.replaceItems(
                DocumentOwnerType.TAX_DOCUMENT, doc.getId(), request.lineItems());
        recalcTotals(doc, items);
        doc = taxDocumentRepository.save(doc);

        return toResponse(doc, items);
    }

    @Transactional(readOnly = true)
    public TaxDocumentResponse findById(Long id) {
        TaxDocument doc = getOwned(id);
        return toResponse(doc, lineItemSupport.find(DocumentOwnerType.TAX_DOCUMENT, doc.getId()));
    }

    @Transactional(readOnly = true)
    public Page<TaxDocumentResponse> findAll(Pageable pageable) {
        Page<TaxDocument> page = tenantSupport.isAdmin()
                ? taxDocumentRepository.findAll(pageable)
                : taxDocumentRepository.findByWorkspace_IdIn(tenantSupport.currentUserWorkspaceIds(), pageable);
        return page.map(doc -> toResponse(doc, lineItemSupport.find(DocumentOwnerType.TAX_DOCUMENT, doc.getId())));
    }

    public DocumentPaymentResponse addPayment(Long taxDocumentId, DocumentPaymentRequest request) {
        TaxDocument doc = getOwned(taxDocumentId);
        if (doc.getStatus() == DocumentStatus.CANCELLED) {
            throw new NotEditableException("Cannot record a payment against a cancelled document");
        }
        DocumentPayment payment = new DocumentPayment();
        payment.setTaxDocument(doc);
        payment.setPaymentMethod(request.paymentMethod());
        payment.setAmount(request.amount());
        payment.setReceivedAt(request.receivedAt() != null ? request.receivedAt() : LocalDate.now());
        return DocumentPaymentResponse.from(paymentRepository.save(payment));
    }

    void recalcTotals(TaxDocument doc, List<DocumentLineItem> items) {
        TotalsBreakdown breakdown = totalsCalculator.calculate(
                lineItemSupport.toRequests(items), doc.getDiscountType(), doc.getDiscountValue(),
                doc.getVatType(), doc.getRoundingMode());
        doc.setNetTotal(breakdown.netTotal());
        doc.setVatRate(breakdown.vatRate());
        doc.setVatAmount(breakdown.vatAmount());
        doc.setPreRoundTotal(breakdown.preRoundTotal());
        doc.setRoundingDelta(breakdown.roundingDelta());
        doc.setGrossTotal(breakdown.grossTotal());
    }

    /** Used by {@link ConversionService} to persist the {@code sourcePaymentRequestId} link right
     * after {@link #createEntity}, as its own committed step. */
    TaxDocument saveLinkedDraft(TaxDocument draft) {
        return taxDocumentRepository.save(draft);
    }

    /** Package-visible for {@link TaxDocumentIssueService} and {@link ConversionService}. */
    TaxDocument getOwned(Long id) {
        if (tenantSupport.isAdmin()) {
            return taxDocumentRepository.findByIdFetched(id)
                    .orElseThrow(() -> new ResourceNotFoundException("TaxDocument", "id", id));
        }
        return taxDocumentRepository.findByIdAndWorkspaceIds(id, tenantSupport.currentUserWorkspaceIds())
                .orElseThrow(() -> new CrossTenantAccessException("Tax document not accessible"));
    }

    private void requireDraft(TaxDocument doc) {
        if (doc.getStatus() == DocumentStatus.ISSUED) {
            String label = doc.getNumber() != null ? doc.getNumber() : String.valueOf(doc.getId());
            throw new ImmutableDocumentException("Tax document " + label + " is issued and immutable");
        }
        if (doc.getStatus() == DocumentStatus.CANCELLED) {
            throw new NotEditableException("Tax document is cancelled");
        }
    }

    TaxDocumentResponse toResponse(TaxDocument doc, List<DocumentLineItem> items) {
        List<LineItemResponse> lineItems = items.stream().map(LineItemResponse::from).toList();
        List<DocumentPaymentResponse> payments = paymentRepository.findByTaxDocument_Id(doc.getId())
                .stream().map(DocumentPaymentResponse::from).toList();
        return TaxDocumentResponse.from(doc, lineItems, payments);
    }

    /** Bypasses tenant scoping entirely — used only by the public share-link download path
     * ({@code DocumentShareService.renderPdfForShareLink}), where a valid, unexpired share token has
     * already proven the anonymous caller may access this exact document. Never call this from
     * anything reachable by an authenticated user's own request. */
    TaxDocument getTrusted(Long id) {
        return taxDocumentRepository.findByIdFetched(id)
                .orElseThrow(() -> new ResourceNotFoundException("TaxDocument", "id", id));
    }

    /** Companion to {@link #getTrusted}, for the same public share-link path. */
    TaxDocumentResponse toResponseForShareLink(TaxDocument doc) {
        return toResponse(doc, lineItemSupport.find(DocumentOwnerType.TAX_DOCUMENT, doc.getId()));
    }
}
