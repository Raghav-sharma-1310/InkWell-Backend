/*
 * This source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.exception;

public class UnauthorizedException extends RuntimeException {
    // Provides unauthorized exception wiring so the framework can apply the expected runtime behavior.
    public UnauthorizedException(String message) {
        super(message);
    }
}
