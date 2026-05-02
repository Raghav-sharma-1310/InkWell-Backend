/*
 * This source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

/* This class groups global exception handler test behavior so the module keeps a clear responsibility. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlesIllegalArgumentConflictAndBadRequest() {
        assertThat(handler.handleIllegalArgumentException(new IllegalArgumentException("already subscribed")).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handleIllegalArgumentException(new IllegalArgumentException("bad token")).getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handlesIllegalStateRuntimeAndValidation() {
        assertThat(handler.handleIllegalStateException(new IllegalStateException("pending")).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT);
        assertThat(handler.handleRuntimeException(new RuntimeException("boom")).getBody().message())
            .contains("An error occurred: boom");

        MethodArgumentNotValidException validation = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(validation.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(new ObjectError("subscribeRequest", "email required")));

        assertThat(handler.handleValidationExceptions(validation).getBody().message())
            .contains("Validation error: email required");
    }
}
