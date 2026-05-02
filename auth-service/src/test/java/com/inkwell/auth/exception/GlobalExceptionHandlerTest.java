/*
 * Codex documentation pass: this source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups global exception handler test behavior so the module keeps a clear responsibility. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should handle ResourceNotFoundException")
    void handleNotFound() {
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(new ResourceNotFoundException("User not found"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("message", "User not found");
        assertThat(response.getBody()).containsEntry("status", 404);
    }

    @Test
    @DisplayName("Should handle BadRequestException")
    void handleBadRequest() {
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(new BadRequestException("Invalid input"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Invalid input");
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException")
    void handleIllegalArgument() {
        ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(new IllegalArgumentException("Bad arg"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("Should handle UnauthorizedException")
    void handleUnauthorized() {
        ResponseEntity<Map<String, Object>> response = handler.handleUnauthorized(new UnauthorizedException("Not authorized"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("message", "Not authorized");
    }

    @Test
    @DisplayName("Should handle AccessDeniedException")
    void handleAccessDenied() {
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(new AccessDeniedException("Forbidden"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("Should handle TooManyRequestsException")
    void handleTooManyRequests() {
        ResponseEntity<Map<String, Object>> response = handler.handleTooManyRequests(new TooManyRequestsException("Rate limited"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException")
    void handleValidation() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "Email is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(ex);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("errors");
        assertThat(response.getBody()).containsKey("fieldErrors");
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void handleGeneric() {
        ResponseEntity<Map<String, Object>> response = handler.handleGeneric(new Exception("Unexpected error"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("message", "Unexpected error");
    }
}
