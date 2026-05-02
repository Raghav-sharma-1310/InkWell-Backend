/*
 * Codex documentation pass: this source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/* This record groups feedback message response behavior so the module keeps a clear responsibility. */
public record FeedbackMessageResponse(
    UUID messageId,
    UUID senderId,
    String senderName,
    String senderRole,
    String content,
    LocalDateTime sentAt
) {
}
