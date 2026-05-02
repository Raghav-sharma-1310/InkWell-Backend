/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto;

import java.time.Instant;

/* This record groups api response behavior so the module keeps a clear responsibility. */
public record ApiResponse<T>(
    Instant timestamp,
    String message,
    T data
) {
    // Defines of so related behavior stays grouped in one place.
    public static <T> ApiResponse<T> of(String message, T data) {
        return new ApiResponse<>(Instant.now(), message, data);
    }
}
