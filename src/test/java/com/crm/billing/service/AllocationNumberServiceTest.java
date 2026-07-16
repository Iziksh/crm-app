package com.crm.billing.service;

import com.crm.billing.entity.AllocationThreshold;
import com.crm.billing.entity.TaxDocument;
import com.crm.billing.enums.AllocationStatus;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.enums.VatType;
import com.crm.billing.exception.AllocationRequiredButFailedException;
import com.crm.billing.exception.MissingCustomerTaxIdException;
import com.crm.billing.repository.AllocationThresholdRepository;
import com.crm.domain.entity.Account;
import com.crm.domain.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocationNumberServiceTest {

    @Mock AllocationThresholdRepository thresholdRepository;
    @Mock IsraelTaxAuthorityService taxAuthorityService;

    private AllocationNumberService service;

    @BeforeEach
    void setUp() {
        service = new AllocationNumberService(thresholdRepository, taxAuthorityService);
    }

    private TaxDocument document(DocumentType type, VatType vatType, BigDecimal netTotal, String customerTaxId) {
        TaxDocument doc = new TaxDocument();
        doc.setId(1L);
        doc.setDocumentType(type);
        doc.setVatType(vatType);
        doc.setNetTotal(netTotal);
        doc.setCurrency("ILS");
        doc.setDocumentDate(LocalDate.of(2026, 7, 1));
        Workspace workspace = new Workspace();
        workspace.setId(10L);
        doc.setWorkspace(workspace);
        Account account = new Account();
        account.setTaxId(customerTaxId);
        doc.setAccount(account);
        return doc;
    }

    private void stubThreshold(BigDecimal amount) {
        AllocationThreshold threshold = new AllocationThreshold();
        threshold.setThresholdAmount(amount);
        threshold.setEffectiveDate(LocalDate.of(2026, 6, 1));
        when(thresholdRepository.findTopByEffectiveDateLessThanEqualOrderByEffectiveDateDesc(any()))
                .thenReturn(Optional.of(threshold));
    }

    @Test
    void belowThreshold_notRequired() {
        stubThreshold(new BigDecimal("5000.00"));
        TaxDocument doc = document(DocumentType.TAX_INVOICE_RECEIPT, VatType.STANDARD, new BigDecimal("1000.00"), "123456789");

        service.apply(doc);

        assertThat(doc.getAllocationStatus()).isEqualTo(AllocationStatus.NOT_REQUIRED);
        assertThat(doc.getAllocationNumber()).isNull();
    }

    @Test
    void zeroVat_skipped_regardlessOfAmount() {
        TaxDocument doc = document(DocumentType.TAX_INVOICE_RECEIPT, VatType.ZERO, new BigDecimal("50000.00"), "123456789");

        service.apply(doc);

        assertThat(doc.getAllocationStatus()).isEqualTo(AllocationStatus.NOT_REQUIRED);
    }

    @Test
    void nonAllocationEligibleType_skipped() {
        TaxDocument doc = document(DocumentType.PROFORMA, VatType.STANDARD, new BigDecimal("50000.00"), "123456789");

        service.apply(doc);

        assertThat(doc.getAllocationStatus()).isEqualTo(AllocationStatus.NOT_REQUIRED);
    }

    @Test
    void missingCustomerTaxId_blocksAboveThreshold() {
        stubThreshold(new BigDecimal("5000.00"));
        TaxDocument doc = document(DocumentType.TAX_INVOICE_RECEIPT, VatType.STANDARD, new BigDecimal("10000.00"), null);

        assertThrows(MissingCustomerTaxIdException.class, () -> service.apply(doc));
        assertThat(doc.getAllocationStatus()).isEqualTo(AllocationStatus.FAILED);
    }

    @Test
    void aboveThreshold_requestsAllocationNumber() {
        stubThreshold(new BigDecimal("5000.00"));
        TaxDocument doc = document(DocumentType.TAX_INVOICE_RECEIPT, VatType.STANDARD, new BigDecimal("10000.00"), "123456789");
        when(taxAuthorityService.requestAllocationNumber(any())).thenReturn(new AllocationResult(true, "ALLOC-1", null));

        service.apply(doc);

        assertThat(doc.getAllocationStatus()).isEqualTo(AllocationStatus.ISSUED);
        assertThat(doc.getAllocationNumber()).isEqualTo("ALLOC-1");
    }

    @Test
    void authorityFailure_setsFailedStatus_andThrows() {
        stubThreshold(new BigDecimal("5000.00"));
        TaxDocument doc = document(DocumentType.TAX_INVOICE_RECEIPT, VatType.STANDARD, new BigDecimal("10000.00"), "123456789");
        when(taxAuthorityService.requestAllocationNumber(any())).thenReturn(new AllocationResult(false, null, "sandbox down"));

        assertThrows(AllocationRequiredButFailedException.class, () -> service.apply(doc));
        assertThat(doc.getAllocationStatus()).isEqualTo(AllocationStatus.FAILED);
        assertThat(doc.getAllocationNumber()).isNull();
    }

    @Test
    void retry_isIdempotent_whenAlreadyIssued() {
        TaxDocument doc = document(DocumentType.TAX_INVOICE_RECEIPT, VatType.STANDARD, new BigDecimal("10000.00"), "123456789");
        doc.setAllocationStatus(AllocationStatus.ISSUED);
        doc.setAllocationNumber("ALLOC-EXISTING");

        service.apply(doc);

        assertThat(doc.getAllocationNumber()).isEqualTo("ALLOC-EXISTING");
        // no threshold/tax-authority lookups needed — short-circuited before any repository/service call.
    }
}
