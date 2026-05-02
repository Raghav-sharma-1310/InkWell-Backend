/*
 * This source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.exception;

import com.inkwell.newsletter.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
/* This class groups global exception handler behavior so the module keeps a clear responsibility. */
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    // Defines handle illegal argument exception so related behavior stays grouped in one place.
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        
        if (message != null && message.contains("already subscribed")) {
            status = HttpStatus.CONFLICT;
        }
        
        return ResponseEntity.status(status).body(new ApiResponse<>(java.time.Instant.now(), message, null));
    }
    
    @ExceptionHandler(IllegalStateException.class)
    // Defines handle illegal state exception so related behavior stays grouped in one place.
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>(java.time.Instant.now(), ex.getMessage(), null));
    }

    @ExceptionHandler(RuntimeException.class)
    // Defines handle runtime exception so related behavior stays grouped in one place.
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(java.time.Instant.now(), "An error occurred: " + ex.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    // Defines handle validation exceptions so related behavior stays grouped in one place.
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(java.time.Instant.now(), "Validation error: " + ex.getBindingResult().getAllErrors().get(0).getDefaultMessage(), null));
    }
}
