package com.crm.billing.service;

import com.crm.billing.dto.TaxDocumentCreateRequest;
import com.crm.billing.entity.TaxDocument;
import com.crm.billing.enums.DocumentStatus;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.exception.CrossTenantAccessException;
import com.crm.billing.exception.ImmutableDocumentException;
import com.crm.billing.repository.DocumentPaymentRepository;
import com.crm.billing.repository.TaxDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaxDocumentDraftServiceTest {

    @Mock TaxDocumentRepository taxDocumentRepository;
    @Mock DocumentPaymentRepository paymentRepository;
    @Mock DocumentLineItemSupport lineItemSupport;
    @Mock BillingTenantSupport tenantSupport;
    @Mock TotalsCalculator totalsCalculator;

    private TaxDocumentDraftService service;

    @BeforeEach
    void setUp() {
        service = new TaxDocumentDraftService(taxDocumentRepository, paymentRepository, lineItemSupport,
                tenantSupport, totalsCalculator);
    }

    private TaxDocumentCreateRequest sampleRequest() {
        return new TaxDocumentCreateRequest(DocumentType.TAX_INVOICE_RECEIPT, 2L, "ILS",
                LocalDate.of(2026, 7, 1), null, null, null, null, null, List.of());
    }

    @Test
    void update_rejected_whenIssued() {
        TaxDocument doc = new TaxDocument();
        doc.setId(1L);
        doc.setStatus(DocumentStatus.ISSUED);
        doc.setNumber("INV-2026-00001");

        when(tenantSupport.isAdmin()).thenReturn(false);
        when(tenantSupport.currentUserWorkspaceIds()).thenReturn(List.of(10L));
        when(taxDocumentRepository.findByIdAndWorkspaceIds(1L, List.of(10L))).thenReturn(Optional.of(doc));

        assertThrows(ImmutableDocumentException.class, () -> service.update(1L, sampleRequest()));
    }

    @Test
    void getOwned_crossTenant_blocked() {
        when(tenantSupport.isAdmin()).thenReturn(false);
        when(tenantSupport.currentUserWorkspaceIds()).thenReturn(List.of(10L));
        when(taxDocumentRepository.findByIdAndWorkspaceIds(1L, List.of(10L))).thenReturn(Optional.empty());

        assertThrows(CrossTenantAccessException.class, () -> service.findById(1L));
    }
}
