/*
 * This source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
/* This class groups global exception handler behavior so the module keeps a clear responsibility. */
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    // Defines not found so related behavior stays grouped in one place.
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    // Defines bad request so related behavior stays grouped in one place.
    public ResponseEntity<Map<String, Object>> badRequest(RuntimeException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    // Defines forbidden so related behavior stays grouped in one place.
    public ResponseEntity<Map<String, Object>> forbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // Defines validation so related behavior stays grouped in one place.
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> body = base(HttpStatus.BAD_REQUEST, "Validation failed");
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Handles invalid enum values sent by the frontend (e.g., EARLY_ACCESS).
     * Jackson fails to deserialize unknown enum values and throws this exception.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> messageNotReadable(HttpMessageNotReadableException ex) {
        String message = "Invalid request body";
        Throwable cause = ex.getCause();
        if (cause != null && cause.getMessage() != null) {
            String causeMsg = cause.getMessage();
            // Detect enum deserialization failures and provide a clear message
            if (causeMsg.contains("not one of the values accepted")) {
                message = "Invalid value in request: " + extractEnumError(causeMsg);
            } else {
                message = "Malformed request: " + causeMsg.substring(0, Math.min(causeMsg.length(), 200));
            }
        }
        log.warn("Request deserialization failed: {}", message);
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Handles database constraint violations (e.g., data truncation from enum mismatch).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> dataIntegrity(DataIntegrityViolationException ex) {
        String message = "Database constraint violation";
        Throwable root = ex.getRootCause();
        if (root != null && root.getMessage() != null) {
            String rootMsg = root.getMessage();
            if (rootMsg.contains("Data truncated")) {
                message = "Invalid data value for a database column. A possible enum mismatch exists — please run the migration script.";
                log.error("Data truncation error (likely enum mismatch): {}", rootMsg);
            } else {
                message = "Database error: " + rootMsg.substring(0, Math.min(rootMsg.length(), 200));
                log.error("Data integrity violation: {}", rootMsg);
            }
        }
        return build(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    // Defines generic so related behavior stays grouped in one place.
    public ResponseEntity<Map<String, Object>> generic(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    // Defines build so related behavior stays grouped in one place.
    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(base(status, message));
    }

    // Defines base so related behavior stays grouped in one place.
    private Map<String, Object> base(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return body;
    }

    /**
     * Extracts a user-friendly message from Jackson's enum deserialization error.
     */
    private String extractEnumError(String causeMessage) {
        // Typical message: "Cannot deserialize value of type `...PostVisibility` from String \"EARLY_ACCESS\": not one of the values accepted..."
        try {
            int fromIdx = causeMessage.indexOf("from String \"");
            int typeIdx = causeMessage.indexOf("type `");
            if (fromIdx > 0 && typeIdx > 0) {
                String value = causeMessage.substring(fromIdx + 13, causeMessage.indexOf("\"", fromIdx + 13));
                String type = causeMessage.substring(typeIdx + 6, causeMessage.indexOf("`", typeIdx + 6));
                String simpleName = type.contains(".") ? type.substring(type.lastIndexOf('.') + 1) : type;
                return "'" + value + "' is not a valid " + simpleName + ". Use PUBLIC or PREMIUM.";
            }
        } catch (Exception ex) {
            log.debug("Failed to extract enum error detail: {}", ex.getMessage());
        }
        return causeMessage.substring(0, Math.min(causeMessage.length(), 200));
    }
}
