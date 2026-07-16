package com.crm.billing.service;

/**
 * ASSUMPTION — flagged per the prompt's own instruction: no {@code IsraelTaxAuthorityService}
 * (or any OAuth2 client, allocation-number logic, sandbox/prod switch, or token cache) exists
 * anywhere in this codebase or was supplied as a real API spec. This interface's single method
 * is invented to match the shape described ("requestAllocationNumber(AllocationRequest)").
 *
 * A real client for חשבוניות ישראל / שע"מ should be added later as a separate
 * {@code @ConditionalOnProperty(name = "tax.authority.mode", havingValue = "live")} bean
 * implementing this same interface — callers ({@code AllocationNumberService}) never change.
 */
public interface IsraelTaxAuthorityService {
    AllocationResult requestAllocationNumber(AllocationRequest request);
}
