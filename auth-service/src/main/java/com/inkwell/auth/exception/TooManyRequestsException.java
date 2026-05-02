/*
 * Codex documentation pass: this source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
/* This class groups too many requests exception behavior so the module keeps a clear responsibility. */
public class TooManyRequestsException extends RuntimeException {
    // Defines too many requests exception so related behavior stays grouped in one place.
    public TooManyRequestsException(String message) {
        super(message);
    }
}
