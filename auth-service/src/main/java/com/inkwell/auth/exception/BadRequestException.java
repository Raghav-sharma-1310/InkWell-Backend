/*
 * This source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.exception;

public class BadRequestException extends RuntimeException {
    // Defines bad request exception so related behavior stays grouped in one place.
    public BadRequestException(String message) {
        super(message);
    }
}
