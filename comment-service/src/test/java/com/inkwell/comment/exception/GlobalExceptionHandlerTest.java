/*
 * This source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/* This class groups global exception handler test behavior so the module keeps a clear responsibility. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should handle ResourceNotFoundException")
    void handleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");
        ResponseEntity<Map<String, Object>> response = handler.notFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404)
                                      .containsEntry("message", "Not found");
    }

    @Test
    @DisplayName("Should handle ForbiddenException")
    void handleForbidden() {
        ForbiddenException ex = new ForbiddenException("Forbidden");
        ResponseEntity<Map<String, Object>> response = handler.forbidden(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("status", 403)
                                      .containsEntry("message", "Forbidden");
    }

    @Test
    @DisplayName("Should handle generic Exception")
    void handleGenericException() {
        Exception ex = new Exception("Server Error");
        ResponseEntity<Map<String, Object>> response = handler.generic(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500)
                                      .containsEntry("message", "Server Error");
    }
}
