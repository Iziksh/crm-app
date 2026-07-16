package com.crm.exception;

public class InvalidOtpException extends RuntimeException {
    public InvalidOtpException() {
        super("Invalid or expired verification code");
    }
}
