package com.crm.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class LastAdminException extends RuntimeException {
    public LastAdminException() {
        super("Cannot remove or demote the last company admin");
    }
}
