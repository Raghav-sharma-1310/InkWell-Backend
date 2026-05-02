/*
 * Codex documentation pass: this source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.exception;

public class ResourceNotFoundException extends RuntimeException {
    // Defines resource not found exception so related behavior stays grouped in one place.
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
