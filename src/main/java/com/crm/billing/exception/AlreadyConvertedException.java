package com.crm.billing.exception;

public class AlreadyConvertedException extends RuntimeException {
    public AlreadyConvertedException(String message) {
        super(message);
    }
}
