package com.crm.billing.service;

import com.crm.billing.entity.PaymentRequestSequence;
import com.crm.billing.repository.PaymentRequestSequenceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Split into its own bean (rather than a private method on {@code PaymentRequestNumberingService})
 * so {@code REQUIRES_NEW} actually takes effect — Spring's proxy-based @Transactional is bypassed on
 * self-invocation within the same class, and a failed insert here must not abort the caller's
 * enclosing transaction (Postgres aborts the whole transaction after any failed statement).
 */
@Component
public class PaymentRequestSequenceInitializer {

    private final PaymentRequestSequenceRepository sequenceRepository;

    public PaymentRequestSequenceInitializer(PaymentRequestSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureExists(Long workspaceId, Integer year) {
        if (sequenceRepository.existsByWorkspaceIdAndYear(workspaceId, year)) {
            return;
        }
        try {
            PaymentRequestSequence seq = new PaymentRequestSequence();
            seq.setWorkspaceId(workspaceId);
            seq.setYear(year);
            seq.setLastNumber(0);
            sequenceRepository.saveAndFlush(seq);
        } catch (DataIntegrityViolationException e) {
            // Lost a create race to a concurrent request — the row exists now, which is all we need.
        }
    }
}
