/*
 * Codex documentation pass: this source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.request;

import jakarta.validation.constraints.Size;

/* This record groups admin remark request behavior so the module keeps a clear responsibility. */
public record AdminRemarkRequest(
    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    String remarks
) {
}
