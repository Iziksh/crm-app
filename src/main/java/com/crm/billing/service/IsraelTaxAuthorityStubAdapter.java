package com.crm.billing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Sandbox stand-in for the real Israel Tax Authority OAuth2 client (see the assumption note on
 * {@link IsraelTaxAuthorityService}). Always succeeds with a synthetic allocation number, so
 * development/testing can exercise the full issue/allocation flow without real credentials. */
@Service
@ConditionalOnProperty(name = "tax.authority.mode", havingValue = "stub", matchIfMissing = true)
public class IsraelTaxAuthorityStubAdapter implements IsraelTaxAuthorityService {

    private static final Logger log = LoggerFactory.getLogger(IsraelTaxAuthorityStubAdapter.class);

    @Override
    public AllocationResult requestAllocationNumber(AllocationRequest request) {
        log.info("[SANDBOX] Requesting allocation number: workspace={} document={} type={} netAmount={} {}",
                request.workspaceId(), request.taxDocumentId(), request.documentTypeCode(),
                request.netAmountBeforeVat(), request.currency());
        String allocationNumber = "SANDBOX-%d-%d".formatted(request.taxDocumentId(), System.nanoTime());
        return new AllocationResult(true, allocationNumber, null);
    }
}
