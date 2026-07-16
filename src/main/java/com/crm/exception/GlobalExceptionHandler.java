package com.crm.exception;

import com.crm.timetracking.exception.AttendanceValidationException;
import com.crm.billing.exception.AllocationRequiredButFailedException;
import com.crm.billing.exception.AlreadyConvertedException;
import com.crm.billing.exception.CrossTenantAccessException;
import com.crm.billing.exception.IllegalConversionTargetForTaxStatusException;
import com.crm.billing.exception.ImmutableDocumentException;
import com.crm.billing.exception.MissingCustomerTaxIdException;
import com.crm.billing.exception.NotEditableException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateEmailException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid"));
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), 400, "Bad Request", "Validation failed", req.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "Unauthorized", "Invalid username or password", req.getRequestURI()));
    }

    @ExceptionHandler(NoDeliverableEmailException.class)
    public ResponseEntity<ErrorResponse> handleNoDeliverableEmail(NoDeliverableEmailException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "Unauthorized", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtp(InvalidOtpException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "Unauthorized", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(403, "Forbidden", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(AttendanceValidationException.class)
    public ResponseEntity<ErrorResponse> handleAttendanceValidation(
            AttendanceValidationException ex, HttpServletRequest req) {
        // 422 Unprocessable Entity: request is syntactically valid but fails semantic rules
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(422, "Unprocessable Entity", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(InvitationInvalidException.class)
    public ResponseEntity<ErrorResponse> handleInvitationInvalid(InvitationInvalidException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Bad Request", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler({NotEditableException.class, AlreadyConvertedException.class, ImmutableDocumentException.class})
    public ResponseEntity<ErrorResponse> handleBillingConflict(RuntimeException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, "Conflict", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler({IllegalConversionTargetForTaxStatusException.class, MissingCustomerTaxIdException.class})
    public ResponseEntity<ErrorResponse> handleBillingRuleViolation(RuntimeException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(422, "Unprocessable Entity", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(AllocationRequiredButFailedException.class)
    public ResponseEntity<ErrorResponse> handleAllocationFailed(AllocationRequiredButFailedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse(502, "Bad Gateway", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(CrossTenantAccessException.class)
    public ResponseEntity<ErrorResponse> handleCrossTenantAccess(CrossTenantAccessException ex, HttpServletRequest req) {
        // 404, not 403: don't let status code reveal that the resource exists in another tenant.
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, "Not Found", "Resource not found", req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Internal Server Error", "An unexpected error occurred", req.getRequestURI()));
    }
}
