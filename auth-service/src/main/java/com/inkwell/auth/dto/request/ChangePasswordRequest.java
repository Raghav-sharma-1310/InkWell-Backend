/*
 * Codex documentation pass: this source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/* This record groups change password request behavior so the module keeps a clear responsibility. */
public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank
    @Size(min = 8, max = 64)
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+$",
        message = "Password must contain upper, lower case letters and a digit")
    String newPassword
) {
}
