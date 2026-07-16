package com.crm.billing.exception;

/** A workspace member attempted to read/write a billing document belonging to another workspace.
 * Mapped to 404 (not 403) so cross-tenant existence can't be enumerated via status code. */
public class CrossTenantAccessException extends RuntimeException {
    public CrossTenantAccessException(String message) {
        super(message);
    }
}
