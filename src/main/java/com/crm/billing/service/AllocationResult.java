package com.crm.billing.service;

/** Result of {@link IsraelTaxAuthorityService#requestAllocationNumber}. */
public record AllocationResult(
        boolean success,
        String allocationNumber,
        String errorMessage
) {}
