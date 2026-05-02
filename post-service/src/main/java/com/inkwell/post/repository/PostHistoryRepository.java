/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.repository;

import com.inkwell.post.entity.PostHistory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups post history repository behavior so the module keeps a clear responsibility. */
public interface PostHistoryRepository extends JpaRepository<PostHistory, UUID> {
    Optional<PostHistory> findByUserIdAndPostId(UUID userId, UUID postId);
    Page<PostHistory> findByUserIdOrderByViewedAtDesc(UUID userId, Pageable pageable);
    void deleteByUserId(UUID userId);
    void deleteByPostId(UUID postId);
}
