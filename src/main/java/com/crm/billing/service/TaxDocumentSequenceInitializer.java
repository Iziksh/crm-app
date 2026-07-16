package com.crm.billing.service;

import com.crm.billing.entity.TaxDocumentSequence;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.repository.TaxDocumentSequenceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** See {@link PaymentRequestSequenceInitializer} for why this must be its own bean rather than a
 * private method on {@code TaxDocumentNumberingService}. */
@Component
public class TaxDocumentSequenceInitializer {

    private final TaxDocumentSequenceRepository sequenceRepository;

    public TaxDocumentSequenceInitializer(TaxDocumentSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureExists(Long workspaceId, DocumentType documentType, Integer year) {
        if (sequenceRepository.existsByWorkspaceIdAndDocumentTypeAndYear(workspaceId, documentType, year)) {
            return;
        }
        try {
            TaxDocumentSequence seq = new TaxDocumentSequence();
            seq.setWorkspaceId(workspaceId);
            seq.setDocumentType(documentType);
            seq.setYear(year);
            seq.setLastNumber(0);
            sequenceRepository.saveAndFlush(seq);
        } catch (DataIntegrityViolationException e) {
            // Lost a create race to a concurrent request — the row exists now, which is all we need.
        }
    }
}
