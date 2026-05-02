/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/* This record groups reset password request behavior so the module keeps a clear responsibility. */
public record ResetPasswordRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 64) String newPassword
) {
}
