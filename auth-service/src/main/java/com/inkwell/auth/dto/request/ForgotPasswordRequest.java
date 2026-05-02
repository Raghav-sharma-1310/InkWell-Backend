/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/* This record groups forgot password request behavior so the module keeps a clear responsibility. */
public record ForgotPasswordRequest(@NotBlank @Email String email) {
}
