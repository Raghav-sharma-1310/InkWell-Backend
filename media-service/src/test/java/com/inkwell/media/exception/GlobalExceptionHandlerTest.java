/*
 * This source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/* This class groups global exception handler test behavior so the module keeps a clear responsibility. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsNotFoundAndGenericExceptions() {
        var notFound = handler.notFound(new ResourceNotFoundException("missing media"));
        var generic = handler.generic(new RuntimeException("storage failed"));

        assertThat(notFound.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFound.getBody()).containsEntry("status", 404).containsEntry("message", "missing media");
        assertThat(generic.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(generic.getBody()).containsEntry("status", 500).containsEntry("message", "storage failed");
    }
}
