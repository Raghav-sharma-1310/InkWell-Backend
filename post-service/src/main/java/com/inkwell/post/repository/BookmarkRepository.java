/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.repository;

import com.inkwell.post.entity.Bookmark;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/* This interface groups bookmark repository behavior so the module keeps a clear responsibility. */
public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {
    Optional<Bookmark> findByUserIdAndPostId(UUID userId, UUID postId);
    boolean existsByUserIdAndPostId(UUID userId, UUID postId);
    List<Bookmark> findByUserIdOrderByCreatedAtDesc(UUID userId);
    void deleteByUserIdAndPostId(UUID userId, UUID postId);
    void deleteByPostId(UUID postId);

    @Query("SELECT b.postId FROM Bookmark b WHERE b.userId = :userId")
    List<UUID> findPostIdsByUserId(@Param("userId") UUID userId);
}
