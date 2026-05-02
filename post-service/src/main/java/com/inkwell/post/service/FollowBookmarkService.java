/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.service;

import com.inkwell.post.dto.response.PageResponse;
import com.inkwell.post.dto.response.PostResponse;
import com.inkwell.post.entity.Bookmark;
import com.inkwell.post.entity.Follow;
import com.inkwell.post.entity.Post;
import com.inkwell.post.entity.PostHistory;
import com.inkwell.post.enumtype.PostStatus;
import com.inkwell.post.exception.ResourceNotFoundException;
import com.inkwell.post.repository.BookmarkRepository;
import com.inkwell.post.repository.FollowRepository;
import com.inkwell.post.repository.PostHistoryRepository;
import com.inkwell.post.repository.PostRepository;
import com.inkwell.post.security.GatewayUserPrincipal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
/* This class groups follow bookmark service behavior so the module keeps a clear responsibility. */
public class FollowBookmarkService {

    private final FollowRepository followRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final PostHistoryRepository postHistoryRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    // Defines toggle follow so related behavior stays grouped in one place.
    public Map<String, Object> toggleFollow(UUID authorId, GatewayUserPrincipal principal) {
        UUID followerId = principal.userUuid();
        if (followerId.equals(authorId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }

        boolean exists = followRepository.existsByFollowerIdAndFollowedId(followerId, authorId);
        if (exists) {
            followRepository.deleteByFollowerIdAndFollowedId(followerId, authorId);
        } else {
            followRepository.save(Follow.builder()
                .followerId(followerId)
                .followedId(authorId)
                .build());

            try {
                rabbitTemplate.convertAndSend(
                    "inkwell.exchange",
                    "author.followed",
                    Map.of(
                        "followerId", followerId.toString(),
                        "followedId", authorId.toString(),
                        "followerName", principal.username()
                    )
                );
            } catch (Exception ex) {
                // RabbitMQ may be unavailable; follow is still recorded locally
                java.util.logging.Logger.getLogger(getClass().getName()).fine("Follow event publish failed: " + ex.getMessage());
            }
        }

        long count = followRepository.countByFollowedId(authorId);
        return Map.of("following", !exists, "followersCount", count);
    }

    @Transactional(readOnly = true)
    // Performs the get following ids workflow so callers do not duplicate this logic.
    public List<UUID> getFollowingIds(GatewayUserPrincipal principal) {
        return followRepository.findByFollowerId(principal.userUuid())
            .stream()
            .map(Follow::getFollowedId)
            .toList();
    }

    @Transactional(readOnly = true)
    // Performs the get follow status workflow so callers do not duplicate this logic.
    public Map<String, Object> getFollowStatus(UUID authorId, GatewayUserPrincipal principal) {
        boolean following = followRepository.existsByFollowerIdAndFollowedId(
            principal.userUuid(), authorId
        );
        long count = followRepository.countByFollowedId(authorId);
        return Map.of("following", following, "followersCount", count);
    }

    @Transactional(readOnly = true)
    // Performs the get followers count workflow so callers do not duplicate this logic.
    public long getFollowersCount(UUID authorId) {
        return followRepository.countByFollowedId(authorId);
    }

    @Transactional(readOnly = true)
    // Performs the get my followers workflow so callers do not duplicate this logic.
    public List<Map<String, Object>> getMyFollowers(GatewayUserPrincipal principal) {
        return followRepository.findByFollowedId(principal.userUuid()).stream()
            .map(f -> Map.<String, Object>of(
                "followerId", f.getFollowerId().toString(),
                "followedAt", f.getCreatedAt().toString()
            ))
            .toList();
    }

    @Transactional
    // Performs the toggle bookmark workflow so callers do not duplicate this logic.
    public Map<String, Object> toggleBookmark(UUID postId, GatewayUserPrincipal principal) {
        UUID userId = principal.userUuid();

        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        boolean exists = bookmarkRepository.existsByUserIdAndPostId(userId, post.getPostId());
        if (exists) {
            bookmarkRepository.deleteByUserIdAndPostId(userId, postId);
        } else {
            bookmarkRepository.save(Bookmark.builder()
                .userId(userId)
                .postId(postId)
                .build());
        }

        return Map.of("bookmarked", !exists);
    }

    @Transactional(readOnly = true)
    // Performs the get bookmarked posts workflow so callers do not duplicate this logic.
    public List<PostResponse> getBookmarkedPosts(GatewayUserPrincipal principal) {
        List<UUID> postIds = bookmarkRepository.findPostIdsByUserId(principal.userUuid());
        if (postIds.isEmpty()) {
            return List.of();
        }

        return postRepository.findAllById(postIds).stream()
            .filter(p -> p.getStatus() == PostStatus.PUBLISHED)
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    // Performs the is bookmarked workflow so callers do not duplicate this logic.
    public boolean isBookmarked(UUID postId, GatewayUserPrincipal principal) {
        return bookmarkRepository.existsByUserIdAndPostId(principal.userUuid(), postId);
    }

    @Transactional(readOnly = true)
    // Performs the get history workflow so callers do not duplicate this logic.
    public PageResponse<PostResponse> getHistory(GatewayUserPrincipal principal, int page, int size) {
        Page<PostHistory> historyPage = postHistoryRepository.findByUserIdOrderByViewedAtDesc(
            principal.userUuid(),
            PageRequest.of(page, size)
        );

        List<PostResponse> posts = historyPage.getContent().stream()
            .map(h -> postRepository.findById(h.getPostId()).orElse(null))
            .filter(Objects::nonNull)
            .map(this::toResponse)
            .toList();

        return new PageResponse<>(
            posts,
            historyPage.getNumber(),
            historyPage.getSize(),
            historyPage.getTotalElements(),
            historyPage.getTotalPages(),
            historyPage.isFirst(),
            historyPage.isLast()
        );
    }

    @Transactional
    // Defines record history so related behavior stays grouped in one place.
    public void recordHistory(String slug, GatewayUserPrincipal principal) {
        if (!"PRO".equalsIgnoreCase(principal.subscriptionTier()) || !"ACTIVE".equalsIgnoreCase(principal.subscriptionStatus())) {
            throw new com.inkwell.post.exception.ForbiddenException("Premium subscription required for reading history");
        }
        
        Post post = postRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
            
        UUID uId = principal.userUuid();
        UUID pId = post.getPostId();
        
        PostHistory history = postHistoryRepository.findByUserIdAndPostId(uId, pId)
            .orElseGet(() -> PostHistory.builder().userId(uId).postId(pId).build());
        
        history.setViewedAt(java.time.LocalDateTime.now());
        postHistoryRepository.save(history);
    }

    @Transactional
    // Defines clear history so related behavior stays grouped in one place.
    public void clearHistory(GatewayUserPrincipal principal) {
        postHistoryRepository.deleteByUserId(principal.userUuid());
    }

    @Transactional
    // Performs the delete history item workflow so callers do not duplicate this logic.
    public void deleteHistoryItem(UUID postId, GatewayUserPrincipal principal) {
        PostHistory history = postHistoryRepository.findByUserIdAndPostId(principal.userUuid(), postId)
            .orElseThrow(() -> new ResourceNotFoundException("History record not found"));
        postHistoryRepository.delete(history);
    }

    // Defines to response so related behavior stays grouped in one place.
    private PostResponse toResponse(Post p) {
        return new PostResponse(
            p.getPostId(),
            p.getAuthorId(),
            p.getTitle(),
            p.getSlug(),
            p.getContent(),
            p.getExcerpt(),
            p.getFeaturedImageUrl(),
            p.getStatus(),
            p.getReadTimeMin(),
            p.getViewCount(),
            p.getLikesCount(),
            p.getCategorySlug(),
            p.getTagSlugs() != null ? p.getTagSlugs() : java.util.Set.of(),
            p.getCreatedAt(),
            p.getUpdatedAt(),
            p.getPublishedAt(),
            p.isFeatured(),
            p.isPinned(),
            p.getVisibility(),
            p.getScheduledAt()
        );
    }
}