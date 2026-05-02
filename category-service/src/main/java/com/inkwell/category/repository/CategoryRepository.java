/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.repository;

import com.inkwell.category.entity.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups category repository behavior so the module keeps a clear responsibility. */
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);
    List<Category> findByParentCategoryId(UUID parentCategoryId);
    
    // For Home Page
    List<Category> findTop5ByPostCountGreaterThanOrderByPostCountDescNameAsc(Long postCount);
    
    // For Explore Page
    List<Category> findByPostCountGreaterThanOrderByNameAsc(Long postCount);
}
