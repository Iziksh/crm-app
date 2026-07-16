package com.crm.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }

    /** Use for non-user entities (Contact, Account, …) so the message doesn't imply a user-account
     * conflict when it's actually a duplicate record within that entity's own table. */
    public DuplicateEmailException(String entityLabel, String email) {
        super(entityLabel + " with this email already exists: " + email);
    }
}
