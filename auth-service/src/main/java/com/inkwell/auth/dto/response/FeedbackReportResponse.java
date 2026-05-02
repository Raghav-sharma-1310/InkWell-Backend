/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.response;

import com.inkwell.auth.enumtype.FeedbackStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/* This record groups feedback report response behavior so the module keeps a clear responsibility. */
public record FeedbackReportResponse(
    UUID reportId,
    UUID userId,
    String username,
    String email,
    String fullName,
    FeedbackStatus status,
    String pageUrl,
    List<FeedbackMessageResponse> messages,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
