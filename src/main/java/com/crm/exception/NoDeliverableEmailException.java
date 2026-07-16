package com.crm.exception;

public class NoDeliverableEmailException extends RuntimeException {
    public NoDeliverableEmailException(String username) {
        super("No deliverable email is configured for user '" + username + "'; contact an administrator");
    }
}
