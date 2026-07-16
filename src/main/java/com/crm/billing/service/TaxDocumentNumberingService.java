package com.crm.billing.service;

import com.crm.billing.entity.TaxDocumentSequence;
import com.crm.billing.enums.DocumentType;
import com.crm.billing.repository.TaxDocumentSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Sequential, gap-free numbering per (workspace, documentType, year) — burned only at issue time,
 * inside the same transaction as the DRAFT->ISSUED status flip in {@code TaxDocumentIssueService},
 * so a failed issue never leaves a gap. */
@Service
public class TaxDocumentNumberingService {

    private static final Map<DocumentType, String> PREFIXES = Map.of(
            DocumentType.TAX_INVOICE, "INV",
            DocumentType.TAX_INVOICE_RECEIPT, "INVR",
            DocumentType.RECEIPT, "REC",
            DocumentType.PROFORMA, "PRO",
            DocumentType.QUOTE, "QT",
            DocumentType.CREDIT_INVOICE, "CR"
    );

    private final TaxDocumentSequenceRepository sequenceRepository;
    private final TaxDocumentSequenceInitializer sequenceInitializer;

    public TaxDocumentNumberingService(TaxDocumentSequenceRepository sequenceRepository,
                                        TaxDocumentSequenceInitializer sequenceInitializer) {
        this.sequenceRepository = sequenceRepository;
        this.sequenceInitializer = sequenceInitializer;
    }

    @Transactional
    public String nextNumber(Long workspaceId, DocumentType documentType, int year) {
        sequenceInitializer.ensureExists(workspaceId, documentType, year);
        TaxDocumentSequence seq = sequenceRepository.findForUpdate(workspaceId, documentType, year)
                .orElseThrow(() -> new IllegalStateException("Tax document sequence row missing after ensureExists"));
        long next = seq.getLastNumber() + 1;
        seq.setLastNumber(next);
        sequenceRepository.save(seq);
        return "%s-%d-%05d".formatted(PREFIXES.get(documentType), year, next);
    }
}
