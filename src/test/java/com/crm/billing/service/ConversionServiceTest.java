package com.crm.billing.service;

import com.crm.billing.dto.TaxDocumentResponse;
import com.crm.billing.entity.PaymentRequest;
import com.crm.billing.entity.TaxDocument;
import com.crm.billing.enums.DocumentOwnerType;
import com.crm.billing.enums.DocumentStatus;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.enums.PaymentRequestStatus;
import com.crm.billing.exception.IllegalConversionTargetForTaxStatusException;
import com.crm.billing.repository.TaxDocumentRepository;
import com.crm.domain.entity.Account;
import com.crm.domain.entity.Workspace;
import com.crm.domain.enums.WorkspaceTaxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversionServiceTest {

    @Mock PaymentRequestService paymentRequestService;
    @Mock TaxDocumentRepository taxDocumentRepository;
    @Mock TaxDocumentDraftService draftService;
    @Mock TaxDocumentIssueService issueService;
    @Mock DocumentLineItemSupport lineItemSupport;

    private ConversionService service;

    @BeforeEach
    void setUp() {
        service = new ConversionService(paymentRequestService, taxDocumentRepository, draftService, issueService, lineItemSupport);
    }

    private PaymentRequest paymentRequest(WorkspaceTaxStatus taxStatus) {
        PaymentRequest pr = new PaymentRequest();
        pr.setId(1L);
        pr.setStatus(PaymentRequestStatus.OPEN);
        pr.setCurrency("ILS");
        pr.setDocumentDate(LocalDate.of(2026, 7, 1));
        Workspace workspace = new Workspace();
        workspace.setId(10L);
        workspace.setTaxStatus(taxStatus);
        pr.setWorkspace(workspace);
        Account account = new Account();
        account.setId(2L);
        pr.setAccount(account);
        return pr;
    }

    @Test
    void exemptDealer_canOnlyConvertToReceipt() {
        PaymentRequest pr = paymentRequest(WorkspaceTaxStatus.EXEMPT_DEALER);
        when(paymentRequestService.getOwned(1L)).thenReturn(pr);
        when(taxDocumentRepository.findBySourcePaymentRequestId(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalConversionTargetForTaxStatusException.class,
                () -> service.convert(1L, DocumentType.PROFORMA));
    }

    @Test
    void authorizedDealer_cannotConvertToReceipt() {
        PaymentRequest pr = paymentRequest(WorkspaceTaxStatus.AUTHORIZED_DEALER);
        when(paymentRequestService.getOwned(1L)).thenReturn(pr);
        when(taxDocumentRepository.findBySourcePaymentRequestId(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalConversionTargetForTaxStatusException.class,
                () -> service.convert(1L, DocumentType.RECEIPT));
    }

    @Test
    void authorizedDealer_canConvertToTaxInvoiceReceipt() {
        PaymentRequest pr = paymentRequest(WorkspaceTaxStatus.AUTHORIZED_DEALER);
        when(paymentRequestService.getOwned(1L)).thenReturn(pr);
        when(taxDocumentRepository.findBySourcePaymentRequestId(1L)).thenReturn(Optional.empty());
        when(lineItemSupport.find(any(), eq(1L))).thenReturn(List.of());
        when(lineItemSupport.toRequests(any())).thenReturn(List.of());

        TaxDocument draft = new TaxDocument();
        draft.setId(5L);
        when(draftService.createEntity(eq(pr.getWorkspace()), any())).thenReturn(draft);
        when(draftService.saveLinkedDraft(draft)).thenReturn(draft);

        TaxDocument issued = new TaxDocument();
        issued.setId(5L);
        issued.setStatus(DocumentStatus.ISSUED);
        when(taxDocumentRepository.findByIdFetched(5L)).thenReturn(Optional.of(issued));
        when(draftService.toResponse(eq(issued), any())).thenReturn(TaxDocumentResponse.from(issued, List.of(), List.of()));

        service.convert(1L, DocumentType.TAX_INVOICE_RECEIPT);

        verify(issueService).issue(5L);
        verify(paymentRequestService).markConverted(1L, 5L);
        assertThat(draft.getSourcePaymentRequestId()).isEqualTo(1L);
    }

    @Test
    void convert_isIdempotent_whenAlreadyConverted() {
        PaymentRequest pr = paymentRequest(WorkspaceTaxStatus.AUTHORIZED_DEALER);
        pr.setConvertedDocumentId(99L);
        when(paymentRequestService.getOwned(1L)).thenReturn(pr);

        TaxDocument existing = new TaxDocument();
        existing.setId(99L);
        when(taxDocumentRepository.findByIdFetched(99L)).thenReturn(Optional.of(existing));
        when(draftService.toResponse(eq(existing), any())).thenReturn(TaxDocumentResponse.from(existing, List.of(), List.of()));

        service.convert(1L, DocumentType.TAX_INVOICE_RECEIPT);

        verify(draftService, never()).createEntity(any(), any());
        verifyNoInteractions(issueService);
    }
}
