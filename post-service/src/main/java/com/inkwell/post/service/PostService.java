/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.service;

import com.inkwell.post.client.CategoryClient;
import com.inkwell.post.client.TaxonomySyncRequest;
import com.inkwell.post.dto.request.SavePostRequest;
import com.inkwell.post.dto.response.LikeResponse;
import com.inkwell.post.dto.response.PageResponse;
import com.inkwell.post.dto.response.PostResponse;
import com.inkwell.post.entity.Post;
import com.inkwell.post.entity.PostHistory;
import com.inkwell.post.entity.PostLike;
import com.inkwell.post.enumtype.PostStatus;
import com.inkwell.post.enumtype.PostVisibility;
import com.inkwell.post.exception.ForbiddenException;
import com.inkwell.post.exception.ResourceNotFoundException;
import com.inkwell.post.repository.BookmarkRepository;
import com.inkwell.post.repository.PostHistoryRepository;
import com.inkwell.post.repository.PostLikeRepository;
import com.inkwell.post.repository.PostRepository;
import com.inkwell.post.security.GatewayUserPrincipal;
import com.inkwell.post.util.SecurityUtils;
import com.inkwell.post.util.HtmlSanitizer;
import com.inkwell.post.util.ReadTimeUtil;
import com.inkwell.post.util.SlugUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
/* This class groups post service behavior so the module keeps a clear responsibility. */
public class PostService {

    private static final String POST_NOT_FOUND = "Post not found";
    private static final String EXCHANGE_NAME = "inkwell.exchange";
    private static final String KEY_POST_ID = "postId";
    private static final String KEY_AUTHOR_ID = "authorId";

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostHistoryRepository postHistoryRepository;
    private final BookmarkRepository bookmarkRepository;
    private final HtmlSanitizer htmlSanitizer;
    private final RabbitTemplate rabbitTemplate;
    private final CategoryClient categoryClient;

    @Transactional
    // Performs the create post workflow so callers do not duplicate this logic.
    public PostResponse createPost(GatewayUserPrincipal principal, SavePostRequest request) {
        if (!principal.isAuthorOrAdmin()) {
            throw new ForbiddenException("Author access required");
        }

        Post post = applyChanges(Post.builder().authorId(principal.userUuid()).build(), request);
        Post saved = postRepository.save(post);
        syncTaxonomy(saved);
        maybePublish(saved);
        return toResponse(saved);
    }

    @Transactional
    // Performs the update post workflow so callers do not duplicate this logic.
    public PostResponse updatePost(UUID postId, GatewayUserPrincipal principal, SavePostRequest request) {
        Post post = requireOwnedOrAdmin(postId, principal);
        Post updated = applyChanges(post, request);
        Post saved = postRepository.save(updated);
        syncTaxonomy(saved);
        maybePublish(saved);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    // Defines public feed so related behavior stays grouped in one place.
    public PageResponse<PostResponse> publicFeed(int page, int size, String categorySlug, String tagSlug, String query) {
        Page<Post> result = postRepository.search(
            PostStatus.PUBLISHED,
            normalizeForSearch(categorySlug),
            normalizeForSearch(tagSlug),
            normalizeForSearch(query),
            pageRequest(page, size)
        );
        return toPage(result);
    }

    @Transactional(readOnly = true)
    // Provides author posts wiring so the framework can apply the expected runtime behavior.
    public PageResponse<PostResponse> authorPosts(GatewayUserPrincipal principal, int page, int size) {
        return toPage(postRepository.findByAuthorId(principal.userUuid(), pageRequest(page, size)));
    }

    @Transactional
    // Performs the get by slug workflow so callers do not duplicate this logic.
    public PostResponse getBySlug(String slug) {
        Post post = postRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));

        if (post.getStatus() == PostStatus.PUBLISHED) {
            post.setViewCount((post.getViewCount() != null ? post.getViewCount() : 0L) + 1);
            post = postRepository.save(post);
        }

        GatewayUserPrincipal principal = resolveCurrentPrincipal();

        enforceAccessControl(post, principal);

        PostResponse response = toResponse(post);
        return canReadFullContent(post, principal) ? response : applyPremiumLock(response);
    }

    // Defines resolve current principal so related behavior stays grouped in one place.
    private GatewayUserPrincipal resolveCurrentPrincipal() {
        try {
            return SecurityUtils.currentPrincipal();
        } catch (Exception ex) {
            log.trace("No authenticated user for slug view: {}", ex.getMessage());
            return null;
        }
    }

    // Defines enforce access control so related behavior stays grouped in one place.
    private void enforceAccessControl(Post post, GatewayUserPrincipal principal) {
        if (post.getStatus() != PostStatus.PUBLISHED) {
            boolean canView = principal != null
                && (principal.isAdmin() || post.getAuthorId().equals(principal.userUuid()));
            if (!canView) {
                throw new ResourceNotFoundException(POST_NOT_FOUND);
            }
        }
    }

    // Defines can read full content so related behavior stays grouped in one place.
    private boolean canReadFullContent(Post post, GatewayUserPrincipal principal) {
        if (post.getVisibility() != PostVisibility.PREMIUM) {
            return true;
        }
        return principal != null
            && ("PRO".equalsIgnoreCase(principal.subscriptionTier())
                || principal.isAdmin()
                || post.getAuthorId().equals(principal.userUuid()));
    }

    // Defines apply premium lock so related behavior stays grouped in one place.
    private PostResponse applyPremiumLock(PostResponse response) {
        String lockedMessage = "<div class='premium-lock'><h2>Premium Content</h2><p>Upgrade to Pro to read the full article.</p></div>";
        return new PostResponse(
            response.postId(), response.authorId(), response.title(), response.slug(),
            response.excerpt() + lockedMessage, response.excerpt(), response.featuredImageUrl(),
            response.status(), response.readTimeMin(), response.viewCount(), response.likesCount(),
            response.categorySlug(), response.tagSlugs(), response.createdAt(), response.updatedAt(),
            response.publishedAt(), response.featured(), response.pinned(),
            response.visibility(), response.scheduledAt()
        );
    }

    @Transactional(readOnly = true)
    // Performs the get by id workflow so callers do not duplicate this logic.
    public PostResponse getById(UUID postId, GatewayUserPrincipal principal) {
        return toResponse(requireOwnedOrAdmin(postId, principal));
    }

    @Transactional(readOnly = true)
    // Performs the get meta workflow so callers do not duplicate this logic.
    public PostResponse getMeta(UUID postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));
        return toResponse(post);
    }

    @Transactional
    // Defines toggle like so related behavior stays grouped in one place.
    public LikeResponse toggleLike(UUID postId, GatewayUserPrincipal principal) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));

        boolean liked = !postLikeRepository.existsByPostIdAndUserId(postId, principal.userUuid());

        if (liked) {
            postLikeRepository.save(new PostLike(postId, principal.userUuid(), LocalDateTime.now()));
        } else {
            postLikeRepository.deleteByPostIdAndUserId(postId, principal.userUuid());
        }

        post.setLikesCount(postLikeRepository.countByPostId(postId));
        postRepository.save(post);

        return new LikeResponse(liked, post.getLikesCount());
    }

    @Transactional
    // Performs the delete post workflow so callers do not duplicate this logic.
    public void deletePost(UUID postId, GatewayUserPrincipal principal) {
        Post post = requireOwnedOrAdmin(postId, principal);
        postRepository.delete(post);

        rabbitTemplate.convertAndSend(
            EXCHANGE_NAME,
            "post.deleted",
            Map.of(
                KEY_POST_ID, postId.toString(),
                KEY_AUTHOR_ID, post.getAuthorId().toString()
            )
        );
    }

    @Transactional
    // Defines feature post so related behavior stays grouped in one place.
    public PostResponse featurePost(UUID postId, boolean featured) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));
        post.setFeatured(featured);
        return toResponse(postRepository.save(post));
    }

    /**
     * Admin-only hard delete with cascade cleanup of all related data.
     */
    @Transactional
    public void adminDeletePost(UUID postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));

        // Cascade delete related data
        postLikeRepository.deleteByPostId(postId);
        bookmarkRepository.deleteByPostId(postId);
        postHistoryRepository.deleteByPostId(postId);

        postRepository.delete(post);

        rabbitTemplate.convertAndSend(
            EXCHANGE_NAME,
            "post.deleted",
            Map.of(
                KEY_POST_ID, postId.toString(),
                KEY_AUTHOR_ID, post.getAuthorId().toString()
            )
        );

        log.info("Admin deleted post {} ({})", postId, post.getTitle());
    }

    @Transactional(readOnly = true)
    // Defines admin search so related behavior stays grouped in one place.
    public PageResponse<PostResponse> adminSearch(int page, int size, String status, String query) {
        PostStatus parsed = (status == null || status.isBlank()) ? null : PostStatus.valueOf(status);
        return toPage(postRepository.search(parsed, "", "", normalizeForSearch(query), pageRequest(page, size)));
    }

    // Defines apply changes so related behavior stays grouped in one place.
    private Post applyChanges(Post post, SavePostRequest request) {
        String cleanContent = htmlSanitizer.sanitize(request.content());

        post.setTitle(request.title().trim());
        post.setSlug(uniqueSlug(post.getPostId(), request.title()));
        post.setContent(cleanContent);
        post.setExcerpt(
            request.excerpt() == null || request.excerpt().isBlank()
                ? excerpt(cleanContent)
                : request.excerpt()
        );
        post.setFeaturedImageUrl(request.featuredImageUrl());
        post.setStatus(request.status());
        post.setReadTimeMin(ReadTimeUtil.estimate(cleanContent));
        post.setCategorySlug(normalize(request.categorySlug()));
        post.setTagSlugs(request.tagSlugs() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(request.tagSlugs()));
        post.setFeatured(request.featured());
        post.setPinned(request.pinned());
        post.setVisibility(request.visibility() != null ? request.visibility() : PostVisibility.PUBLIC);
        post.setScheduledAt(request.scheduledAt());

        if (request.status() == PostStatus.PUBLISHED && post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }
        if (post.getViewCount() == null) {
            post.setViewCount(0L);
        }
        if (post.getLikesCount() == null) {
            post.setLikesCount(0L);
        }

        return post;
    }

    // Defines maybe publish so related behavior stays grouped in one place.
    private void maybePublish(Post post) {
        if (post.getStatus() == PostStatus.PUBLISHED) {
            rabbitTemplate.convertAndSend(
                EXCHANGE_NAME,
                "post.published",
                Map.of(
                    KEY_POST_ID, post.getPostId().toString(),
                    KEY_AUTHOR_ID, post.getAuthorId().toString(),
                    "title", post.getTitle(),
                    "slug", post.getSlug(),
                    "excerpt", post.getExcerpt() != null ? post.getExcerpt() : "",
                    "categorySlug", String.valueOf(post.getCategorySlug())
                )
            );
        }
    }

    @CircuitBreaker(name = "category-service", fallbackMethod = "syncTaxonomyFallback")
    // Performs the sync taxonomy workflow so callers do not duplicate this logic.
    private void syncTaxonomy(Post post) {
        try {
            categoryClient.syncTaxonomy(
                post.getPostId().toString(),
                new TaxonomySyncRequest(
                    post.getPostId(),
                    post.getCategorySlug(),
                    post.getTagSlugs(),
                    post.getStatus() == PostStatus.PUBLISHED
                )
            );
        } catch (Exception ex) {
            log.warn("Category sync failed for post {}: {}", post.getPostId(), ex.getMessage());
        }
    }

    // Performs the sync taxonomy fallback workflow so callers do not duplicate this logic.
    private void syncTaxonomyFallback(Post post, Throwable t) {
        log.warn("Circuit Breaker open/fallback for Category sync: post {}: {}", post.getPostId(), t.getMessage());
    }

    // Defines require owned or admin so related behavior stays grouped in one place.
    private Post requireOwnedOrAdmin(UUID postId, GatewayUserPrincipal principal) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));

        if (!principal.isAdmin() && !post.getAuthorId().equals(principal.userUuid())) {
            throw new ForbiddenException("Post access denied");
        }

        return post;
    }

    // Defines to page so related behavior stays grouped in one place.
    private PageResponse<PostResponse> toPage(Page<Post> result) {
        return new PageResponse<>(
            result.getContent().stream().map(this::toResponse).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages(),
            result.isFirst(),
            result.isLast()
        );
    }

    // Defines to response so related behavior stays grouped in one place.
    private PostResponse toResponse(Post post) {
        Set<String> tagSlugs = post.getTagSlugs() != null ? post.getTagSlugs() : Set.of();

        return new PostResponse(
            post.getPostId(),
            post.getAuthorId(),
            post.getTitle(),
            post.getSlug(),
            post.getContent(),
            post.getExcerpt(),
            post.getFeaturedImageUrl(),
            post.getStatus(),
            post.getReadTimeMin(),
            post.getViewCount(),
            post.getLikesCount(),
            post.getCategorySlug(),
            tagSlugs,
            post.getCreatedAt(),
            post.getUpdatedAt(),
            post.getPublishedAt(),
            post.isFeatured(),
            post.isPinned(),
            post.getVisibility(),
            post.getScheduledAt()
        );
    }

    // Defines unique slug so related behavior stays grouped in one place.
    private String uniqueSlug(UUID currentId, String title) {
        String base = SlugUtil.toSlug(title);
        String slug = base;
        int counter = 1;

        while (postRepository.existsBySlug(slug)) {
            Post existing = postRepository.findBySlug(slug).orElse(null);
            if (existing != null && existing.getPostId().equals(currentId)) {
                return slug;
            }
            slug = base + "-" + counter++;
        }

        return slug;
    }

    // Defines excerpt so related behavior stays grouped in one place.
    private String excerpt(String html) {
        String stripped = html.replaceAll("<[^>]+>", " ")
            .replaceAll("\\s+", " ")
            .trim();
        return stripped.length() <= 180 ? stripped : stripped.substring(0, 177) + "...";
    }

    // Defines page request so related behavior stays grouped in one place.
    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(Math.max(0, page), Math.clamp(size, 1, 50));
    }

    // Defines normalize so related behavior stays grouped in one place.
    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // Defines normalize for search so related behavior stays grouped in one place.
    private String normalizeForSearch(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    @Transactional(readOnly = true)
    // Defines platform stats so related behavior stays grouped in one place.
    public Map<String, Object> platformStats() {
        long totalPublished = postRepository.countByStatus(PostStatus.PUBLISHED);
        Long totalViews = postRepository.sumViewCountByStatus(PostStatus.PUBLISHED);

        return Map.of(
            "totalPublishedPosts", totalPublished,
            "totalViews", totalViews != null ? totalViews : 0L
        );
    }
}