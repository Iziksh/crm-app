package com.crm.billing.service;

import com.crm.billing.dto.IssueResultResponse;
import com.crm.billing.entity.TaxDocument;
import com.crm.billing.enums.AllocationStatus;
import com.crm.billing.enums.DocumentStatus;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.exception.NotEditableException;
import com.crm.billing.repository.TaxDocumentRepository;
import com.crm.domain.entity.Account;
import com.crm.domain.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaxDocumentIssueServiceTest {

    @Mock TaxDocumentDraftService draftService;
    @Mock TaxDocumentRepository taxDocumentRepository;
    @Mock TaxDocumentNumberingService numberingService;
    @Mock AllocationAttemptRunner allocationAttemptRunner;

    private TaxDocumentIssueService service;

    @BeforeEach
    void setUp() {
        service = new TaxDocumentIssueService(draftService, taxDocumentRepository, numberingService, allocationAttemptRunner);
    }

    @Test
    void issue_isIdempotent_whenAlreadyIssued() {
        TaxDocument doc = new TaxDocument();
        doc.setId(1L);
        doc.setStatus(DocumentStatus.ISSUED);
        doc.setNumber("INV-2026-00001");
        doc.setAllocationStatus(AllocationStatus.NOT_REQUIRED);
        when(draftService.getOwned(1L)).thenReturn(doc);

        IssueResultResponse result = service.issue(1L);

        assertThat(result.number()).isEqualTo("INV-2026-00001");
        assertThat(result.message()).isEqualTo("Already issued");
        verifyNoInteractions(allocationAttemptRunner, numberingService);
    }

    @Test
    void issue_rejected_whenCancelled() {
        TaxDocument doc = new TaxDocument();
        doc.setId(1L);
        doc.setStatus(DocumentStatus.CANCELLED);
        when(draftService.getOwned(1L)).thenReturn(doc);

        assertThrows(NotEditableException.class, () -> service.issue(1L));
    }

    @Test
    void issue_burnsNumber_onlyAfterAllocationSucceeds() {
        TaxDocument doc = new TaxDocument();
        doc.setId(1L);
        doc.setStatus(DocumentStatus.DRAFT);
        doc.setDocumentType(DocumentType.RECEIPT);
        doc.setDocumentYear(2026);
        Workspace workspace = new Workspace();
        workspace.setId(10L);
        doc.setWorkspace(workspace);
        doc.setAccount(new Account());

        when(draftService.getOwned(1L)).thenReturn(doc);
        when(allocationAttemptRunner.applyAndPersist(doc)).thenReturn(null);
        when(numberingService.nextNumber(10L, DocumentType.RECEIPT, 2026)).thenReturn("REC-2026-00001");
        when(taxDocumentRepository.save(doc)).thenReturn(doc);

        IssueResultResponse result = service.issue(1L);

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.ISSUED);
        assertThat(result.number()).isEqualTo("REC-2026-00001");
    }

    @Test
    void issue_leavesDraft_whenAllocationFails() {
        TaxDocument doc = new TaxDocument();
        doc.setId(1L);
        doc.setStatus(DocumentStatus.DRAFT);
        doc.setAccount(new Account());

        when(draftService.getOwned(1L)).thenReturn(doc);
        RuntimeException failure = new RuntimeException("allocation failed");
        when(allocationAttemptRunner.applyAndPersist(doc)).thenReturn(failure);

        assertThrows(RuntimeException.class, () -> service.issue(1L));
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(doc.getNumber()).isNull();
        verifyNoInteractions(numberingService);
    }
}
