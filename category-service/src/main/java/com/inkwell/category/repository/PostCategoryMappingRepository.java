/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.repository;

import com.inkwell.category.entity.PostCategoryMapping;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups post category mapping repository behavior so the module keeps a clear responsibility. */
public interface PostCategoryMappingRepository extends JpaRepository<PostCategoryMapping, UUID> {
    List<PostCategoryMapping> findByCategorySlugAndPublishedTrue(String categorySlug);
    List<PostCategoryMapping> findByPostId(UUID postId);
    void deleteByPostId(UUID postId);
    void deleteByCategorySlug(String categorySlug);
}
