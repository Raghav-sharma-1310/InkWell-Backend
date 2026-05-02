/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/* This record groups user response behavior so the module keeps a clear responsibility. */
public record UserResponse(
    UUID userId,
    String username,
    String email,
    String fullName,
    String role,
    String bio,
    String avatarUrl,
    String provider,
    boolean active,
    LocalDateTime createdAt
) {
}
