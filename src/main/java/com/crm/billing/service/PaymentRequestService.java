package com.crm.billing.service;

import com.crm.billing.dto.LineItemResponse;
import com.crm.billing.dto.PaymentRequestCreateRequest;
import com.crm.billing.dto.PaymentRequestResponse;
import com.crm.billing.entity.DocumentLineItem;
import com.crm.billing.entity.PaymentRequest;
import com.crm.billing.enums.DocumentOwnerType;
import com.crm.billing.enums.PaymentRequestStatus;
import com.crm.billing.exception.AlreadyConvertedException;
import com.crm.billing.exception.CrossTenantAccessException;
import com.crm.billing.exception.NotEditableException;
import com.crm.billing.repository.PaymentRequestRepository;
import com.crm.domain.entity.Account;
import com.crm.domain.entity.Workspace;
import com.crm.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** דרישת תשלום — tax-neutral, editable only while OPEN. */
@Service
@Transactional
public class PaymentRequestService {

    private final PaymentRequestRepository paymentRequestRepository;
    private final DocumentLineItemSupport lineItemSupport;
    private final BillingTenantSupport tenantSupport;
    private final PaymentRequestNumberingService numberingService;

    public PaymentRequestService(PaymentRequestRepository paymentRequestRepository,
                                  DocumentLineItemSupport lineItemSupport,
                                  BillingTenantSupport tenantSupport,
                                  PaymentRequestNumberingService numberingService) {
        this.paymentRequestRepository = paymentRequestRepository;
        this.lineItemSupport = lineItemSupport;
        this.tenantSupport = tenantSupport;
        this.numberingService = numberingService;
    }

    public PaymentRequestResponse create(PaymentRequestCreateRequest request) {
        Workspace workspace = tenantSupport.currentWorkspaceOrThrow();
        Account account = tenantSupport.loadCustomer(request.accountId(), workspace);

        PaymentRequest pr = new PaymentRequest();
        pr.setWorkspace(workspace);
        pr.setAccount(account);
        pr.setCurrency(request.currency() != null ? request.currency() : "ILS");
        LocalDate date = request.documentDate() != null ? request.documentDate() : LocalDate.now();
        pr.setDocumentDate(date);
        pr.setFreeText(request.freeText());
        pr.setStatus(PaymentRequestStatus.OPEN);
        pr.setDocumentYear(date.getYear());
        pr.setNumber(numberingService.nextNumber(workspace.getId(), date.getYear()));
        pr = paymentRequestRepository.save(pr);

        List<DocumentLineItem> items = lineItemSupport.replaceItems(
                DocumentOwnerType.PAYMENT_REQUEST, pr.getId(), request.lineItems());
        pr.setTotalAmount(lineItemSupport.sumLineTotals(items));
        pr = paymentRequestRepository.save(pr);

        return toResponse(pr, items);
    }

    public PaymentRequestResponse update(Long id, PaymentRequestCreateRequest request) {
        PaymentRequest pr = getOwned(id);
        if (pr.getStatus() != PaymentRequestStatus.OPEN) {
            throw new NotEditableException(
                    "Payment request " + pr.getNumber() + " is not editable (status=" + pr.getStatus() + ")");
        }
        Account account = tenantSupport.loadCustomer(request.accountId(), pr.getWorkspace());
        pr.setAccount(account);
        if (request.currency() != null) pr.setCurrency(request.currency());
        if (request.documentDate() != null) pr.setDocumentDate(request.documentDate());
        pr.setFreeText(request.freeText());

        List<DocumentLineItem> items = lineItemSupport.replaceItems(
                DocumentOwnerType.PAYMENT_REQUEST, pr.getId(), request.lineItems());
        pr.setTotalAmount(lineItemSupport.sumLineTotals(items));
        pr = paymentRequestRepository.save(pr);

        return toResponse(pr, items);
    }

    @Transactional(readOnly = true)
    public PaymentRequestResponse findById(Long id) {
        PaymentRequest pr = getOwned(id);
        return toResponse(pr, lineItemSupport.find(DocumentOwnerType.PAYMENT_REQUEST, pr.getId()));
    }

    @Transactional(readOnly = true)
    public Page<PaymentRequestResponse> findAll(Pageable pageable) {
        List<Long> ids = tenantSupport.isAdmin() ? null : tenantSupport.currentUserWorkspaceIds();
        Page<PaymentRequest> page = tenantSupport.isAdmin()
                ? paymentRequestRepository.findAll(pageable)
                : paymentRequestRepository.findByWorkspace_IdIn(ids, pageable);
        return page.map(pr -> toResponse(pr, lineItemSupport.find(DocumentOwnerType.PAYMENT_REQUEST, pr.getId())));
    }

    /** Used by {@link ConversionService} once its tax document has been issued. Idempotent: safe to
     * call again with the same convertedDocumentId if a previous attempt crashed after issuing but
     * before this update landed. */
    public void markConverted(Long id, Long convertedDocumentId) {
        PaymentRequest pr = getOwned(id);
        pr.setStatus(PaymentRequestStatus.CONVERTED);
        pr.setConvertedDocumentId(convertedDocumentId);
        paymentRequestRepository.save(pr);
    }

    public void cancel(Long id) {
        PaymentRequest pr = getOwned(id);
        if (pr.getStatus() == PaymentRequestStatus.CONVERTED) {
            throw new AlreadyConvertedException("Cannot cancel payment request " + pr.getNumber() + ": already converted");
        }
        pr.setStatus(PaymentRequestStatus.CANCELLED);
        paymentRequestRepository.save(pr);
    }

    /** Package-visible for {@link ConversionService}: fetches with the same tenant check, without
     * re-implementing it there. */
    PaymentRequest getOwned(Long id) {
        if (tenantSupport.isAdmin()) {
            return paymentRequestRepository.findByIdFetched(id)
                    .orElseThrow(() -> new ResourceNotFoundException("PaymentRequest", "id", id));
        }
        return paymentRequestRepository.findByIdAndWorkspaceIds(id, tenantSupport.currentUserWorkspaceIds())
                .orElseThrow(() -> new CrossTenantAccessException("Payment request not accessible"));
    }

    private PaymentRequestResponse toResponse(PaymentRequest pr, List<DocumentLineItem> items) {
        List<LineItemResponse> lineItems = items.stream().map(LineItemResponse::from).toList();
        return PaymentRequestResponse.from(pr, lineItems);
    }

    /** Bypasses tenant scoping entirely — used only by the public share-link download path
     * ({@code DocumentShareService.renderPdfForShareLink}), where a valid, unexpired share token has
     * already proven the anonymous caller may access this exact document. Never call this from
     * anything reachable by an authenticated user's own request. */
    PaymentRequest getTrusted(Long id) {
        return paymentRequestRepository.findByIdFetched(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentRequest", "id", id));
    }

    /** Companion to {@link #getTrusted}, for the same public share-link path. */
    PaymentRequestResponse toResponseForShareLink(PaymentRequest pr) {
        return toResponse(pr, lineItemSupport.find(DocumentOwnerType.PAYMENT_REQUEST, pr.getId()));
    }
}
