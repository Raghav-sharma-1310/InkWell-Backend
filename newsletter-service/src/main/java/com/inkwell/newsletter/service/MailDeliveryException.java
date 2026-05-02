/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.service;

public class MailDeliveryException extends RuntimeException {

    // Defines mail delivery exception so related behavior stays grouped in one place.
    public MailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
