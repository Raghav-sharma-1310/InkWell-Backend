/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.dto.request;

import com.inkwell.auth.enumtype.Role;
import jakarta.validation.constraints.NotNull;

/* This record groups role update request behavior so the module keeps a clear responsibility. */
public record RoleUpdateRequest(@NotNull Role role) {
}
