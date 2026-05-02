/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* This record groups feedback message request behavior so the module keeps a clear responsibility. */
public record FeedbackMessageRequest(
    @NotBlank(message = "Message is required")
    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    String message,

    @Size(max = 500, message = "Page URL must not exceed 500 characters")
    String pageUrl
) {
}
