/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.request;

import com.inkwell.auth.enumtype.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/* This record groups register request behavior so the module keeps a clear responsibility. */
public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "Username can only contain letters, numbers, underscores, and dots")
    String username,

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+$",
        message = "Password must contain upper, lower case letters and a digit")
    String password,

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Full name must contain only letters and spaces")
    String fullName,

    Role role
) {
}
