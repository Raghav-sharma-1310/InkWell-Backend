/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.request;

import com.inkwell.auth.enumtype.FeedbackStatus;
import jakarta.validation.constraints.NotNull;

/* This record groups feedback status update request behavior so the module keeps a clear responsibility. */
public record FeedbackStatusUpdateRequest(
    @NotNull(message = "Status is required")
    FeedbackStatus status
) {
}
