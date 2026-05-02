/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.repository;

import com.inkwell.post.entity.Follow;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups follow repository behavior so the module keeps a clear responsibility. */
public interface FollowRepository extends JpaRepository<Follow, UUID> {
    Optional<Follow> findByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
    boolean existsByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
    List<Follow> findByFollowerId(UUID followerId);
    List<Follow> findByFollowedId(UUID followedId);
    long countByFollowedId(UUID followedId);
    long countByFollowerId(UUID followerId);
    void deleteByFollowerIdAndFollowedId(UUID followerId, UUID followedId);
}
