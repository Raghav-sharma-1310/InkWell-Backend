/*
 * Codex documentation pass: this source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/* This record groups verify otp request behavior so the module keeps a clear responsibility. */
public record VerifyOtpRequest(@NotBlank @Email String email, @NotBlank String otp) {
}
