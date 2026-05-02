/*
 * This source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.exception;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
/* This class groups global exception handler behavior so the module keeps a clear responsibility. */
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    // Defines not found so related behavior stays grouped in one place.
    public ResponseEntity<Map<String, Object>> notFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("timestamp", Instant.now(), "status", 404, "message", ex.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    // Defines generic so related behavior stays grouped in one place.
    public ResponseEntity<Map<String, Object>> generic(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("timestamp", Instant.now(), "status", 500, "message", ex.getMessage()));
    }
}
