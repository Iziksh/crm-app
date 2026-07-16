package com.crm.billing.service;

import com.crm.billing.entity.TaxDocument;
import com.crm.billing.repository.TaxDocumentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs {@link AllocationNumberService#apply} in its own committed transaction so a FAILED
 * allocation status is durably visible (per the prompt: "on failure keep DRAFT ... surface the
 * error") even though {@link TaxDocumentIssueService#issue} itself must then abort without burning
 * a document number. Must be a separate bean: a caught-and-rethrown exception inside the same
 * @Transactional method would still roll back everything written in that method, including the
 * FAILED status we want to keep.
 */
@Component
public class AllocationAttemptRunner {

    private final TaxDocumentRepository taxDocumentRepository;
    private final AllocationNumberService allocationNumberService;

    public AllocationAttemptRunner(TaxDocumentRepository taxDocumentRepository,
                                    AllocationNumberService allocationNumberService) {
        this.taxDocumentRepository = taxDocumentRepository;
        this.allocationNumberService = allocationNumberService;
    }

    /** Returns the failure (if any) instead of throwing, so this method's own transaction always
     * commits the resulting allocation status; the caller decides whether to propagate the failure. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RuntimeException applyAndPersist(TaxDocument document) {
        RuntimeException failure = null;
        try {
            allocationNumberService.apply(document);
        } catch (RuntimeException e) {
            failure = e;
        }
        taxDocumentRepository.save(document);
        return failure;
    }
}
