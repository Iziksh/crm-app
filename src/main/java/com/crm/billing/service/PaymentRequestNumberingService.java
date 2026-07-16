package com.crm.billing.service;

import com.crm.billing.entity.PaymentRequestSequence;
import com.crm.billing.repository.PaymentRequestSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monotonic, collision-free numbering per (workspace, year) — gaps are legally acceptable for
 * payment requests, unlike tax documents, but the {@code Quote}/{@code SalesOrder} entities'
 * {@code new Random().nextInt(900)+100} numbering is not even collision-safe, so this is a fresh
 * implementation rather than a reuse of anything existing. */
@Service
public class PaymentRequestNumberingService {

    /** Numbers start at 1001 — the prompt's reference screenshot shows "1006" a few requests in. */
    private static final long NUMBER_BASE = 1000L;

    private final PaymentRequestSequenceRepository sequenceRepository;
    private final PaymentRequestSequenceInitializer sequenceInitializer;

    public PaymentRequestNumberingService(PaymentRequestSequenceRepository sequenceRepository,
                                           PaymentRequestSequenceInitializer sequenceInitializer) {
        this.sequenceRepository = sequenceRepository;
        this.sequenceInitializer = sequenceInitializer;
    }

    @Transactional
    public String nextNumber(Long workspaceId, int year) {
        sequenceInitializer.ensureExists(workspaceId, year);
        PaymentRequestSequence seq = sequenceRepository.findForUpdate(workspaceId, year)
                .orElseThrow(() -> new IllegalStateException("Payment request sequence row missing after ensureExists"));
        long next = seq.getLastNumber() + 1;
        seq.setLastNumber(next);
        sequenceRepository.save(seq);
        return String.valueOf(NUMBER_BASE + next);
    }
}
