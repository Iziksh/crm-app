package com.crm.exception;

public class InvitationInvalidException extends RuntimeException {
    public InvitationInvalidException() {
        super("Invalid or expired invitation");
    }
}