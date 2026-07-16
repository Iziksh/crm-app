package com.crm.billing.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Payload for {@link IsraelTaxAuthorityService#requestAllocationNumber}. */
public record AllocationRequest(
        Long workspaceId,
        Long taxDocumentId,
        String documentTypeCode,
        BigDecimal netAmountBeforeVat,
        String currency,
        String customerTaxId,
        LocalDate documentDate
) {}
