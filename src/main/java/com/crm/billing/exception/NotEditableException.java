package com.crm.billing.exception;

/** A PaymentRequest is no longer editable (not OPEN) or a TaxDocument's DRAFT-only field was touched
 * after it was ISSUED — never thrown for ISSUED tax documents themselves, see {@link ImmutableDocumentException}. */
public class NotEditableException extends RuntimeException {
    public NotEditableException(String message) {
        super(message);
    }
}
