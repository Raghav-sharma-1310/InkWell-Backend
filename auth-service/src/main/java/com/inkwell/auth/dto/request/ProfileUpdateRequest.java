/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
/* This class groups profile update request behavior so the module keeps a clear responsibility. */
public class ProfileUpdateRequest {
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Full name must contain only letters and spaces")
    private String fullName;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 80, message = "Username must be between 3 and 80 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "Username can only contain letters, numbers, underscores, and dots")
    private String username;

    @Size(max = 1000, message = "Bio must not exceed 1000 characters")
    private String bio;

    @Pattern(regexp = "^(\\+?\\d{10,15})?$", message = "Phone number must be 10-15 digits, optionally starting with +")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;
}
