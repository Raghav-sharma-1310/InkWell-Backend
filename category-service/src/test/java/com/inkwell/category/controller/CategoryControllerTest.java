/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.controller;

import com.inkwell.category.dto.ApiResponse;
import com.inkwell.category.dto.request.CategoryRequest;
import com.inkwell.category.dto.request.TagRequest;
import com.inkwell.category.dto.request.TaxonomySyncRequest;
import com.inkwell.category.dto.response.CategoryResponse;
import com.inkwell.category.dto.response.TagResponse;
import com.inkwell.category.service.CategoryService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/* This class groups category controller test behavior so the module keeps a clear responsibility. */
class CategoryControllerTest {

    private final CategoryService categoryService = mock(CategoryService.class);
    private final PublicCategoryController publicController = new PublicCategoryController(categoryService);
    private final AdminCategoryController adminController = new AdminCategoryController(categoryService);
    private final InternalCategoryController internalController = new InternalCategoryController(categoryService);
    private final UUID id = UUID.randomUUID();
    private final CategoryResponse category = new CategoryResponse(id, "Tech", "tech", "Description", null, 1L);
    private final TagResponse tag = new TagResponse(id, "Java", "java", 2L);

    @Test
    void publicControllerDelegatesReadOperations() {
        when(categoryService.getCategories()).thenReturn(List.of(category));
        when(categoryService.getTop5Categories()).thenReturn(List.of(category));
        when(categoryService.getActiveCategories()).thenReturn(List.of(category));
        when(categoryService.getTags()).thenReturn(List.of(tag));
        when(categoryService.getActiveTags()).thenReturn(List.of(tag));
        when(categoryService.trendingTags()).thenReturn(List.of(tag));
        when(categoryService.getBySlug("tech")).thenReturn(category);
        when(categoryService.getTagBySlug("java")).thenReturn(tag);
        when(categoryService.getCategoriesByPost(id)).thenReturn(List.of(category));
        when(categoryService.getTagsByPost(id)).thenReturn(List.of(tag));

        assertThat(publicController.categories().data()).containsExactly(category);
        assertThat(publicController.topCategories().message()).isEqualTo("Top categories fetched");
        assertThat(publicController.activeCategories().data()).containsExactly(category);
        assertThat(publicController.tags().data()).containsExactly(tag);
        assertThat(publicController.activeTags().data()).containsExactly(tag);
        assertThat(publicController.trendingTags().data()).containsExactly(tag);
        assertThat(publicController.getCategoryBySlug("tech").data()).isEqualTo(category);
        assertThat(publicController.getTagBySlug("java").data()).isEqualTo(tag);
        assertThat(publicController.getCategoriesByPost(id).data()).containsExactly(category);
        assertThat(publicController.getTagsByPost(id).data()).containsExactly(tag);
    }

    @Test
    void adminControllerDelegatesWriteOperations() {
        CategoryRequest categoryRequest = new CategoryRequest("Tech", "Description", null);
        TagRequest tagRequest = new TagRequest("Java");
        when(categoryService.createCategory(categoryRequest)).thenReturn(category);
        when(categoryService.createTag(tagRequest)).thenReturn(tag);
        when(categoryService.updateCategory(id, categoryRequest)).thenReturn(category);

        assertThat(adminController.createCategory(categoryRequest).data()).isEqualTo(category);
        assertThat(adminController.createTag(tagRequest).data()).isEqualTo(tag);
        assertThat(adminController.updateCategory(id, categoryRequest).message()).isEqualTo("Category updated");

        ApiResponse<Void> deleteCategory = adminController.deleteCategory(id);
        ApiResponse<Void> deleteTag = adminController.deleteTag(id);

        assertThat(deleteCategory.message()).isEqualTo("Category deleted");
        assertThat(deleteTag.message()).isEqualTo("Tag deleted");
        verify(categoryService).deleteCategory(id);
        verify(categoryService).deleteTag(id);
    }

    @Test
    void internalControllerDelegatesSync() {
        TaxonomySyncRequest request = new TaxonomySyncRequest(id, "tech", java.util.Set.of("java"), true);

        ApiResponse<Void> response = internalController.sync(id.toString(), request);

        assertThat(response.message()).isEqualTo("Taxonomy synced");
        verify(categoryService).syncTaxonomy(request);
    }
}
