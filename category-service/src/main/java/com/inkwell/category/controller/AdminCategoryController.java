/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.controller;

import com.inkwell.category.dto.ApiResponse;
import com.inkwell.category.dto.request.CategoryRequest;
import com.inkwell.category.dto.request.TagRequest;
import com.inkwell.category.dto.response.CategoryResponse;
import com.inkwell.category.dto.response.TagResponse;
import com.inkwell.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
/* This class groups admin category controller behavior so the module keeps a clear responsibility. */
public class AdminCategoryController {

    private final CategoryService categoryService;

    @PostMapping("/categories")
    // Performs the create category workflow so callers do not duplicate this logic.
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) { return ApiResponse.of("Category created", categoryService.createCategory(request)); }

    @PostMapping("/tags")
    // Performs the create tag workflow so callers do not duplicate this logic.
    public ApiResponse<TagResponse> createTag(@Valid @RequestBody TagRequest request) { return ApiResponse.of("Tag created", categoryService.createTag(request)); }

    @org.springframework.web.bind.annotation.PutMapping("/categories/{categoryId}")
    // Performs the update category workflow so callers do not duplicate this logic.
    public ApiResponse<CategoryResponse> updateCategory(@org.springframework.web.bind.annotation.PathVariable java.util.UUID categoryId, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.of("Category updated", categoryService.updateCategory(categoryId, request));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/categories/{categoryId}")
    // Performs the delete category workflow so callers do not duplicate this logic.
    public ApiResponse<Void> deleteCategory(@org.springframework.web.bind.annotation.PathVariable java.util.UUID categoryId) {
        categoryService.deleteCategory(categoryId);
        return ApiResponse.of("Category deleted", null);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/tags/{tagId}")
    // Performs the delete tag workflow so callers do not duplicate this logic.
    public ApiResponse<Void> deleteTag(@org.springframework.web.bind.annotation.PathVariable java.util.UUID tagId) {
        categoryService.deleteTag(tagId);
        return ApiResponse.of("Tag deleted", null);
    }
}
