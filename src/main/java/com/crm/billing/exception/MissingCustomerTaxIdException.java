package com.crm.billing.exception;

public class MissingCustomerTaxIdException extends RuntimeException {
    public MissingCustomerTaxIdException(String message) {
        super(message);
    }
}
