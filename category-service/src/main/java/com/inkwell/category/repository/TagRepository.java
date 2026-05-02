/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.repository;

import com.inkwell.category.entity.Tag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups tag repository behavior so the module keeps a clear responsibility. */
public interface TagRepository extends JpaRepository<Tag, UUID> {
    Optional<Tag> findBySlug(String slug);
    List<Tag> findTop10ByOrderByPostCountDesc();
    
    // For Explore Page Tag Dropdown
    List<Tag> findByPostCountGreaterThanOrderByNameAsc(Long postCount);
}
