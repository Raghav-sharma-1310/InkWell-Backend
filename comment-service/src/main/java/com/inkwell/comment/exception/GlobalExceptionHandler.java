/*
 * This source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
/* This class groups global exception handler behavior so the module keeps a clear responsibility. */
public class GlobalExceptionHandler {

    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_STATUS = "status";
    private static final String KEY_MESSAGE = "message";

    @ExceptionHandler(ResourceNotFoundException.class)
    // Defines not found so related behavior stays grouped in one place.
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(KEY_TIMESTAMP, Instant.now(), KEY_STATUS, 404, KEY_MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    // Defines forbidden so related behavior stays grouped in one place.
    public ResponseEntity<Map<String, Object>> forbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(KEY_TIMESTAMP, Instant.now(), KEY_STATUS, 403, KEY_MESSAGE, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    // Defines generic so related behavior stays grouped in one place.
    public ResponseEntity<Map<String, Object>> generic(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(KEY_TIMESTAMP, Instant.now(), KEY_STATUS, 500, KEY_MESSAGE, ex.getMessage()));
    }
}
