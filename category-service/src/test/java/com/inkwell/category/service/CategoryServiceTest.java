/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.service;

import com.inkwell.category.dto.request.CategoryRequest;
import com.inkwell.category.dto.request.TagRequest;
import com.inkwell.category.dto.request.TaxonomySyncRequest;
import com.inkwell.category.dto.response.CategoryResponse;
import com.inkwell.category.dto.response.TagResponse;
import com.inkwell.category.entity.Category;
import com.inkwell.category.entity.PostCategoryMapping;
import com.inkwell.category.entity.PostTagMapping;
import com.inkwell.category.entity.Tag;
import com.inkwell.category.exception.ResourceNotFoundException;
import com.inkwell.category.repository.CategoryRepository;
import com.inkwell.category.repository.PostCategoryMappingRepository;
import com.inkwell.category.repository.PostTagMappingRepository;
import com.inkwell.category.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
/* This class groups category service test behavior so the module keeps a clear responsibility. */
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private TagRepository tagRepository;
    @Mock private PostCategoryMappingRepository postCategoryMappingRepository;
    @Mock private PostTagMappingRepository postTagMappingRepository;

    @InjectMocks private CategoryService categoryService;

    private final UUID categoryId = UUID.randomUUID();
    private final UUID tagId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();

    @Test
    @DisplayName("Should fetch category lists")
    void fetchCategoryLists() {
        Category category = category("Tech", "tech", 2L);
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(categoryRepository.findByPostCountGreaterThanOrderByNameAsc(0L)).thenReturn(List.of(category));
        when(categoryRepository.findTop5ByPostCountGreaterThanOrderByPostCountDescNameAsc(0L)).thenReturn(List.of(category));

        assertThat(categoryService.getCategories()).extracting(CategoryResponse::slug).containsExactly("tech");
        assertThat(categoryService.getActiveCategories()).extracting(CategoryResponse::postCount).containsExactly(2L);
        assertThat(categoryService.getTop5Categories()).extracting(CategoryResponse::name).containsExactly("Tech");
    }

    @Test
    @DisplayName("Should fetch tag lists")
    void fetchTagLists() {
        Tag tag = tag("Spring Boot", "spring-boot", 3L);
        when(tagRepository.findAll()).thenReturn(List.of(tag));
        when(tagRepository.findByPostCountGreaterThanOrderByNameAsc(0L)).thenReturn(List.of(tag));
        when(tagRepository.findTop10ByOrderByPostCountDesc()).thenReturn(List.of(tag));

        assertThat(categoryService.getTags()).extracting(TagResponse::slug).containsExactly("spring-boot");
        assertThat(categoryService.getActiveTags()).extracting(TagResponse::postCount).containsExactly(3L);
        assertThat(categoryService.trendingTags()).extracting(TagResponse::name).containsExactly("Spring Boot");
    }

    @Test
    @DisplayName("Should fetch category and tag by slug")
    void fetchBySlug() {
        when(categoryRepository.findBySlug("tech")).thenReturn(Optional.of(category("Tech", "tech", 1L)));
        when(tagRepository.findBySlug("java")).thenReturn(Optional.of(tag("Java", "java", 1L)));

        assertThat(categoryService.getBySlug("tech").name()).isEqualTo("Tech");
        assertThat(categoryService.getTagBySlug("java").name()).isEqualTo("Java");
    }

    @Test
    @DisplayName("Should fetch categories and tags mapped to a post")
    void fetchTaxonomyByPost() {
        when(postCategoryMappingRepository.findByPostId(postId)).thenReturn(List.of(new PostCategoryMapping(postId, "tech", true)));
        when(categoryRepository.findBySlug("tech")).thenReturn(Optional.of(category("Tech", "tech", 1L)));
        when(postTagMappingRepository.findByPostId(postId)).thenReturn(List.of(
                new PostTagMapping(postId, "java", true),
                new PostTagMapping(postId, "missing", true)
        ));
        when(tagRepository.findBySlug("java")).thenReturn(Optional.of(tag("Java", "java", 1L)));
        when(tagRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThat(categoryService.getCategoriesByPost(postId)).extracting(CategoryResponse::slug).containsExactly("tech");
        assertThat(categoryService.getTagsByPost(postId)).extracting(TagResponse::slug).containsExactly("java");
    }

    @Test
    @DisplayName("Should create category and tag")
    void createCategoryAndTag() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setCategoryId(categoryId);
            return saved;
        });
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> {
            Tag saved = invocation.getArgument(0);
            saved.setTagId(tagId);
            return saved;
        });

        CategoryResponse category = categoryService.createCategory(new CategoryRequest("Data Science", "Articles", null));
        TagResponse tag = categoryService.createTag(new TagRequest("Machine Learning"));

        assertThat(category.slug()).isEqualTo("data-science");
        assertThat(tag.slug()).isEqualTo("machine-learning");
    }

    @Test
    @DisplayName("Should update category")
    void updateCategory() {
        Category category = category("Old", "old", 0L);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.save(category)).thenReturn(category);

        CategoryResponse response = categoryService.updateCategory(categoryId, new CategoryRequest("New Name", "Updated", null));

        assertThat(response.name()).isEqualTo("New Name");
        assertThat(response.slug()).isEqualTo("new-name");
        assertThat(response.description()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("Should delete category and refresh counts")
    void deleteCategory() {
        Category category = category("Tech", "tech", 2L);
        Tag tag = tag("Java", "java", 1L);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(tagRepository.findAll()).thenReturn(List.of(tag));
        when(postCategoryMappingRepository.findByCategorySlugAndPublishedTrue("tech")).thenReturn(List.of(new PostCategoryMapping(postId, "tech", true)));
        when(postTagMappingRepository.findByTagSlugAndPublishedTrue("java")).thenReturn(List.of());

        categoryService.deleteCategory(categoryId);

        verify(postCategoryMappingRepository).deleteByCategorySlug("tech");
        verify(categoryRepository).delete(category);
        verify(categoryRepository).save(category);
        verify(tagRepository).save(tag);
        assertThat(category.getPostCount()).isEqualTo(1L);
        assertThat(tag.getPostCount()).isZero();
    }

    @Test
    @DisplayName("Should delete tag and refresh counts")
    void deleteTag() {
        Category category = category("Tech", "tech", 0L);
        Tag tag = tag("Java", "java", 2L);
        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(tagRepository.findAll()).thenReturn(List.of(tag));
        when(postCategoryMappingRepository.findByCategorySlugAndPublishedTrue("tech")).thenReturn(List.of());
        when(postTagMappingRepository.findByTagSlugAndPublishedTrue("java")).thenReturn(List.of(new PostTagMapping(postId, "java", true)));

        categoryService.deleteTag(tagId);

        verify(postTagMappingRepository).deleteByTagSlug("java");
        verify(tagRepository).delete(tag);
        assertThat(tag.getPostCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should sync taxonomy and create missing terms")
    void syncTaxonomyCreatesMissingTerms() {
        Category category = category("Existing", "existing", 0L);
        Tag tag = tag("Existing Tag", "existing-tag", 0L);
        when(categoryRepository.findBySlug("web-dev")).thenReturn(Optional.empty());
        when(tagRepository.findBySlug("react")).thenReturn(Optional.empty());
        when(tagRepository.findBySlug("existing-tag")).thenReturn(Optional.of(tag));
        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(tagRepository.findAll()).thenReturn(List.of(tag));
        when(postCategoryMappingRepository.findByCategorySlugAndPublishedTrue("existing")).thenReturn(List.of());
        when(postTagMappingRepository.findByTagSlugAndPublishedTrue("existing-tag")).thenReturn(List.of(new PostTagMapping(postId, "existing-tag", true)));

        categoryService.syncTaxonomy(new TaxonomySyncRequest(postId, "Web Dev", Set.of("React", " ", "existing-tag"), true));

        verify(categoryRepository, atLeastOnce()).save(any(Category.class));
        verify(postCategoryMappingRepository).save(any(PostCategoryMapping.class));
        verify(postTagMappingRepository).deleteByPostId(postId);
        verify(postTagMappingRepository, atLeastOnce()).save(any(PostTagMapping.class));
        assertThat(tag.getPostCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should sync taxonomy with blank category and null tags")
    void syncTaxonomyWithBlankCategoryAndNullTags() {
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(tagRepository.findAll()).thenReturn(List.of());

        categoryService.syncTaxonomy(new TaxonomySyncRequest(postId, " ", null, false));

        verify(postCategoryMappingRepository, never()).save(any());
        verify(postTagMappingRepository).deleteByPostId(postId);
    }

    @Test
    @DisplayName("Should handle post deleted event")
    void onPostDeleted() {
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(tagRepository.findAll()).thenReturn(List.of());

        categoryService.onPostDeleted(Map.of("postId", postId.toString()));

        verify(postCategoryMappingRepository).deleteByPostId(postId);
        verify(postTagMappingRepository).deleteByPostId(postId);
    }

    @Test
    @DisplayName("Should ignore malformed post deleted event")
    void onPostDeletedMalformedPayload() {
        categoryService.onPostDeleted(Map.of("postId", "not-a-uuid"));

        verify(postCategoryMappingRepository, never()).deleteByPostId(any());
        verify(postTagMappingRepository, never()).deleteByPostId(any());
    }

    @Test
    @DisplayName("Should throw when deleting non-existent category")
    void deleteCategoryNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    @DisplayName("Should throw when updating non-existent category")
    void updateCategoryNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        CategoryRequest request = new CategoryRequest("name", "desc", null);
        assertThatThrownBy(() -> categoryService.updateCategory(id, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
    }
    
    @Test
    @DisplayName("Should throw when getting non-existent category by slug")
    void getBySlugNotFound() {
        when(categoryRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getBySlug("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Category not found");
    }

    @Test
    @DisplayName("Should throw when getting non-existent tag by slug")
    void getTagBySlugNotFound() {
        when(tagRepository.findBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getTagBySlug("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tag not found");
    }

    @Test
    @DisplayName("Should throw when deleting non-existent tag")
    void deleteTagNotFound() {
        when(tagRepository.findById(tagId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteTag(tagId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Tag not found");
    }

    // Defines category so related behavior stays grouped in one place.
    private Category category(String name, String slug, long postCount) {
        return Category.builder()
                .categoryId(categoryId)
                .name(name)
                .slug(slug)
                .description("Description")
                .postCount(postCount)
                .build();
    }

    // Defines tag so related behavior stays grouped in one place.
    private Tag tag(String name, String slug, long postCount) {
        return Tag.builder()
                .tagId(tagId)
                .name(name)
                .slug(slug)
                .postCount(postCount)
                .build();
    }
}
