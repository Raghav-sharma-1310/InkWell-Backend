/*
 * Codex documentation pass: this source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.response;

import com.inkwell.auth.enumtype.RequestStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/* This record groups author request response behavior so the module keeps a clear responsibility. */
public record AuthorRequestResponse(
    UUID requestId,
    UUID userId,
    String username,
    String email,
    String fullName,
    RequestStatus status,
    String adminRemarks,
    LocalDateTime requestedAt,
    LocalDateTime updatedAt
) {
}
