/*
 * This source file contains application error handling for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.exception;

public class ForbiddenException extends RuntimeException { public ForbiddenException(String message) { super(message); } }
