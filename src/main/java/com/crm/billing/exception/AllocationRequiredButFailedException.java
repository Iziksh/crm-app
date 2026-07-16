package com.crm.billing.exception;

/** Thrown when an allocation number is required but the tax-authority call failed. The caller
 * (TaxDocumentIssueService) catches this, keeps the document in DRAFT with AllocationStatus.FAILED,
 * burns no number, and lets a retry be idempotent. */
public class AllocationRequiredButFailedException extends RuntimeException {
    public AllocationRequiredButFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
