/*
 * This source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.exception;

public class ResourceNotFoundException extends RuntimeException { public ResourceNotFoundException(String message) { super(message); } }
