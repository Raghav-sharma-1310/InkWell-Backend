/*
 * This source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
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

    @Test
    @DisplayName("Should handle bad request exceptions")
    void handleBadRequest() {
        ResponseEntity<Map<String, Object>> response = handler.badRequest(new BadRequestException("Bad input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Bad input");
    }

    @Test
    @DisplayName("Should handle validation errors")
    void handleValidation() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "title", "must not be blank"));
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validationTarget", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.validation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Validation failed");
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertThat(errors).containsEntry("title", "must not be blank");
    }

    @Test
    @DisplayName("Should handle enum deserialization failures")
    void handleEnumDeserializationFailure() {
        RuntimeException cause = new RuntimeException("Cannot deserialize value of type `com.inkwell.post.enumtype.PostVisibility` from String \"EARLY_ACCESS\": not one of the values accepted");
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("bad body", cause, null);

        ResponseEntity<Map<String, Object>> response = handler.messageNotReadable(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).asString().contains("EARLY_ACCESS").contains("PostVisibility");
    }

    @Test
    @DisplayName("Should handle malformed request body")
    void handleMalformedRequestBody() {
        RuntimeException cause = new RuntimeException("Unexpected token");

        ResponseEntity<Map<String, Object>> response = handler.messageNotReadable(new HttpMessageNotReadableException("bad", cause, null));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).asString().contains("Malformed request");
    }

    @Test
    @DisplayName("Should handle data truncation violations")
    void handleDataTruncation() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("bad", new RuntimeException("Data truncated for column"));

        ResponseEntity<Map<String, Object>> response = handler.dataIntegrity(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).asString().contains("Invalid data value");
    }

    @Test
    @DisplayName("Should handle generic data integrity violations")
    void handleDataIntegrity() {
        DataIntegrityViolationException exception = new DataIntegrityViolationException("bad", new RuntimeException("Duplicate key"));

        ResponseEntity<Map<String, Object>> response = handler.dataIntegrity(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("message")).asString().contains("Database error");
    }

    @SuppressWarnings("unused")
    // Performs the validation target workflow so callers do not duplicate this logic.
    private void validationTarget(String title) {
        // Intentionally empty; this reflective test target only supplies a MethodParameter.
    }
}
