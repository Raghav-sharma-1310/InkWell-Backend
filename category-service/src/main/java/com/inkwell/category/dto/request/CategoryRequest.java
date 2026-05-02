/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/* This record groups category request behavior so the module keeps a clear responsibility. */
public record CategoryRequest(@NotBlank @Size(max = 120) String name, @Size(max = 500) String description, UUID parentCategoryId) {}
