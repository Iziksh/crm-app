package com.crm.billing.service;

import com.crm.billing.dto.IssueResultResponse;
import com.crm.billing.entity.TaxDocument;
import com.crm.billing.enums.DocumentStatus;
import com.crm.billing.exception.NotEditableException;
import com.crm.billing.repository.TaxDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** DRAFT -> ISSUED. Idempotent (re-issuing an already-ISSUED document just returns its existing
 * result) and never burns a document number unless allocation (if required) already succeeded. */
@Service
public class TaxDocumentIssueService {

    private final TaxDocumentDraftService draftService;
    private final TaxDocumentRepository taxDocumentRepository;
    private final TaxDocumentNumberingService numberingService;
    private final AllocationAttemptRunner allocationAttemptRunner;

    public TaxDocumentIssueService(TaxDocumentDraftService draftService,
                                    TaxDocumentRepository taxDocumentRepository,
                                    TaxDocumentNumberingService numberingService,
                                    AllocationAttemptRunner allocationAttemptRunner) {
        this.draftService = draftService;
        this.taxDocumentRepository = taxDocumentRepository;
        this.numberingService = numberingService;
        this.allocationAttemptRunner = allocationAttemptRunner;
    }

    @Transactional
    public IssueResultResponse issue(Long id) {
        TaxDocument doc = draftService.getOwned(id);

        if (doc.getStatus() == DocumentStatus.ISSUED) {
            return new IssueResultResponse(doc.getId(), doc.getNumber(), doc.getAllocationStatus(),
                    doc.getAllocationNumber(), "Already issued");
        }
        if (doc.getStatus() == DocumentStatus.CANCELLED) {
            throw new NotEditableException("Cannot issue a cancelled document");
        }

        // Force-initialize the Account association now, inside this open session — AllocationNumberService
        // reads account.getTaxId() from within AllocationAttemptRunner's separate REQUIRES_NEW transaction,
        // where a not-yet-loaded lazy proxy on a detached entity would throw LazyInitializationException.
        if (doc.getAccount() != null) {
            doc.getAccount().getTaxId();
        }

        RuntimeException allocationFailure = allocationAttemptRunner.applyAndPersist(doc);
        if (allocationFailure != null) {
            // Allocation status (FAILED) is already committed by the runner's own transaction;
            // this transaction has burned nothing yet, so the document correctly stays DRAFT.
            throw allocationFailure;
        }

        String number = numberingService.nextNumber(doc.getWorkspace().getId(), doc.getDocumentType(), doc.getDocumentYear());
        doc.setNumber(number);
        doc.setStatus(DocumentStatus.ISSUED);
        doc.setIssuedAt(LocalDateTime.now());
        doc = taxDocumentRepository.save(doc);

        return new IssueResultResponse(doc.getId(), doc.getNumber(), doc.getAllocationStatus(),
                doc.getAllocationNumber(), "Issued");
    }
}
