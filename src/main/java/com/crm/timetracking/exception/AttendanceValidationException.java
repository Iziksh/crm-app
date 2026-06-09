package com.crm.timetracking.exception;

public class AttendanceValidationException extends RuntimeException {
    public AttendanceValidationException(String message) {
        super(message);
    }
}