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
import com.inkwell.category.util.SlugUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
/* This class groups category service behavior so the module keeps a clear responsibility. */
public class CategoryService {

    private static final String CATEGORY_NOT_FOUND = "Category not found";

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PostCategoryMappingRepository postCategoryMappingRepository;
    private final PostTagMappingRepository postTagMappingRepository;

    @Transactional(readOnly = true)
    // Performs the get categories workflow so callers do not duplicate this logic.
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream().map(this::toCategoryResponse).toList();
    }

    @Transactional(readOnly = true)
    // Performs the get active categories workflow so callers do not duplicate this logic.
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByPostCountGreaterThanOrderByNameAsc(0L)
            .stream().map(this::toCategoryResponse).toList();
    }

    @Transactional(readOnly = true)
    // Performs the get top5 categories workflow so callers do not duplicate this logic.
    public List<CategoryResponse> getTop5Categories() {
        return categoryRepository.findTop5ByPostCountGreaterThanOrderByPostCountDescNameAsc(0L)
            .stream().map(this::toCategoryResponse).toList();
    }

    @Transactional(readOnly = true)
    // Performs the get tags workflow so callers do not duplicate this logic.
    public List<TagResponse> getTags() {
        return tagRepository.findAll().stream().map(this::toTagResponse).toList();
    }

    @Transactional(readOnly = true)
    // Performs the get active tags workflow so callers do not duplicate this logic.
    public List<TagResponse> getActiveTags() {
        return tagRepository.findByPostCountGreaterThanOrderByNameAsc(0L)
            .stream().map(this::toTagResponse).toList();
    }

    @Transactional(readOnly = true)
    // Defines trending tags so related behavior stays grouped in one place.
    public List<TagResponse> trendingTags() {
        return tagRepository.findTop10ByOrderByPostCountDesc().stream().map(this::toTagResponse).toList();
    }

    @Transactional(readOnly = true)
    // Performs the get by slug workflow so callers do not duplicate this logic.
    public CategoryResponse getBySlug(String slug) {
        return toCategoryResponse(categoryRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    // Performs the get tag by slug workflow so callers do not duplicate this logic.
    public TagResponse getTagBySlug(String slug) {
        return toTagResponse(tagRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found")));
    }

    @Transactional(readOnly = true)
    // Performs the get tags by post workflow so callers do not duplicate this logic.
    public List<TagResponse> getTagsByPost(UUID postId) {
        return postTagMappingRepository.findByPostId(postId).stream()
            .map(mapping -> tagRepository.findBySlug(mapping.getTagSlug()).orElse(null))
            .filter(java.util.Objects::nonNull)
            .map(this::toTagResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    // Performs the get categories by post workflow so callers do not duplicate this logic.
    public List<CategoryResponse> getCategoriesByPost(UUID postId) {
        return postCategoryMappingRepository.findByPostId(postId).stream()
            .map(mapping -> categoryRepository.findBySlug(mapping.getCategorySlug()).orElse(null))
            .filter(java.util.Objects::nonNull)
            .map(this::toCategoryResponse)
            .toList();
    }

    @Transactional
    // Performs the create category workflow so callers do not duplicate this logic.
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = categoryRepository.save(Category.builder().name(request.name()).slug(SlugUtil.toSlug(request.name())).description(request.description()).parentCategoryId(request.parentCategoryId()).postCount(0L).build());
        return toCategoryResponse(category);
    }

    @Transactional
    // Performs the create tag workflow so callers do not duplicate this logic.
    public TagResponse createTag(TagRequest request) {
        Tag tag = tagRepository.save(Tag.builder().name(request.name()).slug(SlugUtil.toSlug(request.name())).postCount(0L).build());
        return toTagResponse(tag);
    }

    @Transactional
    // Performs the update category workflow so callers do not duplicate this logic.
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND));
        category.setName(request.name());
        category.setSlug(SlugUtil.toSlug(request.name()));
        category.setDescription(request.description());
        category.setParentCategoryId(request.parentCategoryId());
        return toCategoryResponse(categoryRepository.save(category));
    }

    /**
     * Safe delete: removes category-post mappings first (posts are preserved),
     * then deletes the category entity and refreshes all counts.
     */
    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_NOT_FOUND));
        postCategoryMappingRepository.deleteByCategorySlug(category.getSlug());
        categoryRepository.delete(category);
        refreshCounts();
        log.info("Category '{}' deleted safely. Post mappings removed.", category.getName());
    }

    /**
     * Safe delete: removes tag-post mappings first (posts are preserved),
     * then deletes the tag entity and refreshes all counts.
     */
    @Transactional
    public void deleteTag(UUID id) {
        Tag tag = tagRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tag not found"));
        postTagMappingRepository.deleteByTagSlug(tag.getSlug());
        tagRepository.delete(tag);
        refreshCounts();
        log.info("Tag '{}' deleted safely. Post mappings removed.", tag.getName());
    }

    @Transactional
    // Performs the sync taxonomy workflow so callers do not duplicate this logic.
    public void syncTaxonomy(TaxonomySyncRequest request) {
        if (request.categorySlug() != null && !request.categorySlug().isBlank()) {
            String cSlug = SlugUtil.toSlug(request.categorySlug());
            if (categoryRepository.findBySlug(cSlug).isEmpty()) {
                String cName = Arrays.stream(cSlug.split("-"))
                    .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                    .collect(Collectors.joining(" "));
                categoryRepository.save(Category.builder().name(cName).slug(cSlug).postCount(0L).build());
            }
            postCategoryMappingRepository.save(new PostCategoryMapping(request.postId(), cSlug, request.published()));
        }

        postTagMappingRepository.deleteByPostId(request.postId());

        if (request.tagSlugs() != null) {
            request.tagSlugs().forEach(tagSlug -> {
                if (tagSlug == null || tagSlug.isBlank()) return;
                String tSlug = SlugUtil.toSlug(tagSlug);
                if (tagRepository.findBySlug(tSlug).isEmpty()) {
                    String tName = Arrays.stream(tSlug.split("-"))
                        .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
                        .collect(Collectors.joining(" "));
                    tagRepository.save(Tag.builder().name(tName).slug(tSlug).postCount(0L).build());
                }
                postTagMappingRepository.save(new PostTagMapping(request.postId(), tSlug, request.published()));
            });
        }
        refreshCounts();
    }

    /**
     * Listens for post.deleted events from post-service.
     * Removes all category/tag mappings for the deleted post and refreshes counts.
     */
    @RabbitListener(queues = "category-post-deleted-queue")
    @Transactional
    public void onPostDeleted(Map<String, Object> payload) {
        try {
            UUID postId = UUID.fromString(String.valueOf(payload.get("postId")));
            postCategoryMappingRepository.deleteByPostId(postId);
            postTagMappingRepository.deleteByPostId(postId);
            refreshCounts();
            log.info("Post {} deleted — category/tag mappings removed and counts refreshed.", postId);
        } catch (Exception e) {
            log.warn("Failed to handle post.deleted event: {}", e.getMessage());
        }
    }

    // Performs the refresh counts workflow so callers do not duplicate this logic.
    private void refreshCounts() {
        categoryRepository.findAll().forEach(category -> {
            category.setPostCount(postCategoryMappingRepository.findByCategorySlugAndPublishedTrue(category.getSlug()).size());
            categoryRepository.save(category);
        });
        tagRepository.findAll().forEach(tag -> {
            tag.setPostCount(postTagMappingRepository.findByTagSlugAndPublishedTrue(tag.getSlug()).size());
            tagRepository.save(tag);
        });
    }

    // Defines to category response so related behavior stays grouped in one place.
    private CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(category.getCategoryId(), category.getName(), category.getSlug(), category.getDescription(), category.getParentCategoryId(), category.getPostCount());
    }

    // Defines to tag response so related behavior stays grouped in one place.
    private TagResponse toTagResponse(Tag tag) {
        return new TagResponse(tag.getTagId(), tag.getName(), tag.getSlug(), tag.getPostCount());
    }
}
