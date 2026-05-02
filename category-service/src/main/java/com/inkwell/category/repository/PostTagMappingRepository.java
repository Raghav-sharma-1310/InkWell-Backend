/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.repository;

import com.inkwell.category.entity.PostTagMapping;
import com.inkwell.category.entity.PostTagMapping.PostTagMappingId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups post tag mapping repository behavior so the module keeps a clear responsibility. */
public interface PostTagMappingRepository extends JpaRepository<PostTagMapping, PostTagMappingId> {
    List<PostTagMapping> findByTagSlugAndPublishedTrue(String tagSlug);
    List<PostTagMapping> findByPostId(java.util.UUID postId);
    void deleteByPostId(java.util.UUID postId);
    void deleteByTagSlug(String tagSlug);
}
