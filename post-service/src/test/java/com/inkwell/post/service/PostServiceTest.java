/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.service;

import com.inkwell.post.client.CategoryClient;

import com.inkwell.post.dto.request.SavePostRequest;
import com.inkwell.post.dto.response.LikeResponse;
import com.inkwell.post.dto.response.PageResponse;
import com.inkwell.post.dto.response.PostResponse;
import com.inkwell.post.entity.Post;
import com.inkwell.post.entity.PostLike;
import com.inkwell.post.enumtype.PostStatus;
import com.inkwell.post.enumtype.PostVisibility;
import com.inkwell.post.exception.ForbiddenException;
import com.inkwell.post.exception.ResourceNotFoundException;
import com.inkwell.post.repository.PostHistoryRepository;
import com.inkwell.post.repository.PostLikeRepository;
import com.inkwell.post.repository.PostRepository;
import com.inkwell.post.security.GatewayUserPrincipal;
import com.inkwell.post.util.HtmlSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups post service test behavior so the module keeps a clear responsibility. */
class PostServiceTest {

    @Mock private PostRepository postRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private PostHistoryRepository postHistoryRepository;
    @Mock private com.inkwell.post.repository.BookmarkRepository bookmarkRepository;
    @Mock private HtmlSanitizer htmlSanitizer;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private CategoryClient categoryClient;

    @InjectMocks private PostService postService;

    private UUID postId;
    private UUID authorId;
    private Post testPost;
    private GatewayUserPrincipal authorPrincipal;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        authorId = UUID.randomUUID();

        testPost = Post.builder()
                .postId(postId)
                .authorId(authorId)
                .title("Test Post Title")
                .slug("test-post-title")
                .content("<p>Test content</p>")
                .excerpt("Test excerpt")
                .status(PostStatus.PUBLISHED)
                .visibility(PostVisibility.PUBLIC)
                .viewCount(10L)
                .likesCount(5L)
                .tagSlugs(new LinkedHashSet<>(Set.of("java", "spring")))
                .categorySlug("technology")
                .publishedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        authorPrincipal = new GatewayUserPrincipal(
                authorId.toString(), "author", "author@inkwell.com", "AUTHOR", "FREE", null
        );
    }

    @Nested
    @DisplayName("Create and Update Tests")
    /* This class groups create update tests behavior so the module keeps a clear responsibility. */
    class CreateUpdateTests {

        @Test
        @DisplayName("Should create a published post and publish events")
        void createPublishedPost() {
            SavePostRequest request = new SavePostRequest(
                    "New Post",
                    "<script>bad</script><p>Hello world from InkWell</p>",
                    null,
                    "https://img.test/post.png",
                    " tech ",
                    new LinkedHashSet<>(Set.of("java", "spring")),
                    PostStatus.PUBLISHED,
                    true,
                    true,
                    PostVisibility.PREMIUM,
                    null
            );
            when(htmlSanitizer.sanitize(request.content())).thenReturn("<p>Hello world from InkWell</p>");
            when(postRepository.existsBySlug("new-post")).thenReturn(false);
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post post = invocation.getArgument(0);
                post.setPostId(postId);
                return post;
            });

            PostResponse response = postService.createPost(authorPrincipal, request);

            assertThat(response.title()).isEqualTo("New Post");
            assertThat(response.slug()).isEqualTo("new-post");
            assertThat(response.excerpt()).isEqualTo("Hello world from InkWell");
            assertThat(response.categorySlug()).isEqualTo("tech");
            assertThat(response.visibility()).isEqualTo(PostVisibility.PREMIUM);
            assertThat(response.publishedAt()).isNotNull();
            verify(categoryClient).syncTaxonomy(eq(postId.toString()), any());
            verify(rabbitTemplate).convertAndSend(eq("inkwell.exchange"), eq("post.published"), any(Map.class));
        }

        @Test
        @DisplayName("Should reject create for non-author")
        void createPostForbiddenForReader() {
            GatewayUserPrincipal reader = new GatewayUserPrincipal(
                    UUID.randomUUID().toString(), "reader", "reader@test.com", "READER", "FREE", null
            );
            SavePostRequest request = new SavePostRequest("Title", "Content", null, null, null, null, PostStatus.DRAFT, false, false, null, null);

            assertThatThrownBy(() -> postService.createPost(reader, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("Author access required");
            verify(postRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should update an existing post with duplicate slug owned by same post")
        void updatePostAllowsSameSlug() {
            SavePostRequest request = new SavePostRequest(
                    "Test Post Title",
                    "<p>Updated content with many words</p>",
                    "Manual excerpt",
                    null,
                    "",
                    null,
                    PostStatus.DRAFT,
                    false,
                    false,
                    null,
                    LocalDateTime.now().plusDays(1)
            );
            when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
            when(htmlSanitizer.sanitize(request.content())).thenReturn("<p>Updated content with many words</p>");
            when(postRepository.existsBySlug("test-post-title")).thenReturn(true);
            when(postRepository.findBySlug("test-post-title")).thenReturn(Optional.of(testPost));
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

            PostResponse response = postService.updatePost(postId, authorPrincipal, request);

            assertThat(response.excerpt()).isEqualTo("Manual excerpt");
            assertThat(response.tagSlugs()).isEmpty();
            assertThat(response.categorySlug()).isNull();
            assertThat(response.visibility()).isEqualTo(PostVisibility.PUBLIC);
        }

        @Test
        @DisplayName("Should generate numbered slug when another post owns base slug")
        void createPostGeneratesNumberedSlug() {
            Post existing = Post.builder().postId(UUID.randomUUID()).slug("new-post").build();
            SavePostRequest request = new SavePostRequest("New Post", "Content", null, null, null, null, PostStatus.DRAFT, false, false, null, null);
            when(htmlSanitizer.sanitize("Content")).thenReturn("Content");
            when(postRepository.existsBySlug("new-post")).thenReturn(true);
            when(postRepository.findBySlug("new-post")).thenReturn(Optional.of(existing));
            when(postRepository.existsBySlug("new-post-1")).thenReturn(false);
            when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
                Post post = invocation.getArgument(0);
                post.setPostId(postId);
                return post;
            });

            assertThat(postService.createPost(authorPrincipal, request).slug()).isEqualTo("new-post-1");
        }
    }

    @Nested
    @DisplayName("Public Feed Tests")
    /* This class groups public feed tests behavior so the module keeps a clear responsibility. */
    class PublicFeedTests {

        @Test
        @DisplayName("Should return all published posts with no filters")
        void publicFeedNoFilters() {
            Page<Post> page = new PageImpl<>(List.of(testPost));
            when(postRepository.search(eq(PostStatus.PUBLISHED), eq(""), eq(""), eq(""), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<PostResponse> result = postService.publicFeed(0, 10, null, null, null);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).title()).isEqualTo("Test Post Title");
        }

        @Test
        @DisplayName("Should filter by category")
        void publicFeedFilterByCategory() {
            Page<Post> page = new PageImpl<>(List.of(testPost));
            when(postRepository.search(eq(PostStatus.PUBLISHED), eq("technology"), eq(""), eq(""), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<PostResponse> result = postService.publicFeed(0, 10, "technology", null, null);

            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("Should filter by tag")
        void publicFeedFilterByTag() {
            Page<Post> page = new PageImpl<>(List.of(testPost));
            when(postRepository.search(eq(PostStatus.PUBLISHED), eq(""), eq("java"), eq(""), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<PostResponse> result = postService.publicFeed(0, 10, null, "java", null);

            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("Should search by keyword")
        void publicFeedSearchByKeyword() {
            Page<Post> page = new PageImpl<>(List.of(testPost));
            when(postRepository.search(eq(PostStatus.PUBLISHED), eq(""), eq(""), eq("Test"), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<PostResponse> result = postService.publicFeed(0, 10, null, null, "Test");

            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("Should handle combined filters")
        void publicFeedCombinedFilters() {
            Page<Post> page = new PageImpl<>(List.of(testPost));
            when(postRepository.search(eq(PostStatus.PUBLISHED), eq("technology"), eq("java"), eq("Test"), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<PostResponse> result = postService.publicFeed(0, 10, "technology", "java", "Test");

            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("Should return empty when no matching posts")
        void publicFeedEmpty() {
            Page<Post> page = new PageImpl<>(List.of());
            when(postRepository.search(any(), anyString(), anyString(), anyString(), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<PostResponse> result = postService.publicFeed(0, 10, null, null, null);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("Get By Slug Tests")
    /* This class groups get by slug tests behavior so the module keeps a clear responsibility. */
    class GetBySlugTests {

        @Test
        @DisplayName("Should return post by slug and increment view count")
        void getBySlugSuccess() {
            when(postRepository.findBySlug("test-post-title")).thenReturn(Optional.of(testPost));
            when(postRepository.save(any(Post.class))).thenReturn(testPost);

            PostResponse result = postService.getBySlug("test-post-title");

            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("Test Post Title");
            verify(postRepository).save(testPost);
        }

        @Test
        @DisplayName("Should throw when post not found by slug")
        void getBySlugNotFound() {
            when(postRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getBySlug("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Like Toggle Tests")
    /* This class groups like tests behavior so the module keeps a clear responsibility. */
    class LikeTests {

        @Test
        @DisplayName("Should like a post")
        void likePost() {
            when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
            when(postLikeRepository.existsByPostIdAndUserId(postId, authorId)).thenReturn(false);
            when(postLikeRepository.countByPostId(postId)).thenReturn(6L);
            when(postRepository.save(any(Post.class))).thenReturn(testPost);

            LikeResponse result = postService.toggleLike(postId, authorPrincipal);

            assertThat(result.liked()).isTrue();
            verify(postLikeRepository).save(any(PostLike.class));
        }

        @Test
        @DisplayName("Should unlike a post")
        void unlikePost() {
            when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
            when(postLikeRepository.existsByPostIdAndUserId(postId, authorId)).thenReturn(true);
            when(postLikeRepository.countByPostId(postId)).thenReturn(4L);
            when(postRepository.save(any(Post.class))).thenReturn(testPost);

            LikeResponse result = postService.toggleLike(postId, authorPrincipal);

            assertThat(result.liked()).isFalse();
            verify(postLikeRepository).deleteByPostIdAndUserId(postId, authorId);
        }
    }

    @Nested
    @DisplayName("Delete Post Tests")
    /* This class groups delete tests behavior so the module keeps a clear responsibility. */
    class DeleteTests {

        @Test
        @DisplayName("Should delete own post")
        void deleteOwnPost() {
            when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));

            postService.deletePost(postId, authorPrincipal);

            verify(postRepository).delete(testPost);
            verify(rabbitTemplate).convertAndSend(eq("inkwell.exchange"), eq("post.deleted"), any(Map.class));
        }

        @Test
        @DisplayName("Should reject deleting another user's post")
        void deleteOtherUserPost() {
            UUID otherId = UUID.randomUUID();
            GatewayUserPrincipal otherPrincipal = new GatewayUserPrincipal(
                    otherId.toString(), "other", "AUTHOR", "other@inkwell.com", "FREE", null
            );

            when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> postService.deletePost(postId, otherPrincipal))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("Admin Search Tests")
    /* This class groups admin search tests behavior so the module keeps a clear responsibility. */
    class AdminSearchTests {

        @Test
        @DisplayName("Should search with status filter")
        void adminSearchWithStatus() {
            Page<Post> page = new PageImpl<>(List.of(testPost));
            when(postRepository.search(eq(PostStatus.PUBLISHED), eq(""), eq(""), eq(""), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<PostResponse> result = postService.adminSearch(0, 10, "PUBLISHED", null);

            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("Should search without status filter")
        void adminSearchWithoutStatus() {
            Page<Post> page = new PageImpl<>(List.of(testPost));
            when(postRepository.search(isNull(), eq(""), eq(""), eq(""), any(Pageable.class)))
                    .thenReturn(page);

            PageResponse<PostResponse> result = postService.adminSearch(0, 10, null, null);

            assertThat(result.content()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Feature Post Tests")
    /* This class groups feature tests behavior so the module keeps a clear responsibility. */
    class FeatureTests {

        @Test
        @DisplayName("Should feature a post")
        void featurePost() {
            when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
            when(postRepository.save(any(Post.class))).thenReturn(testPost);

            PostResponse result = postService.featurePost(postId, true);

            assertThat(result).isNotNull();
            verify(postRepository).save(any(Post.class));
        }

        @Test
        @DisplayName("Should throw when feature post target is missing")
        void featurePostNotFound() {
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.featurePost(postId, true))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Platform Stats Tests")
    /* This class groups stats tests behavior so the module keeps a clear responsibility. */
    class StatsTests {

        @Test
        @DisplayName("Should return platform stats")
        void platformStatsSuccess() {
            when(postRepository.countByStatus(PostStatus.PUBLISHED)).thenReturn(42L);
            when(postRepository.sumViewCountByStatus(PostStatus.PUBLISHED)).thenReturn(1000L);

            Map<String, Object> stats = postService.platformStats();

            assertThat(stats).containsEntry("totalPublishedPosts", 42L)
                             .containsEntry("totalViews", 1000L);
        }

        @Test
        @DisplayName("Should handle null total views")
        void platformStatsNullViews() {
            when(postRepository.countByStatus(PostStatus.PUBLISHED)).thenReturn(0L);
            when(postRepository.sumViewCountByStatus(PostStatus.PUBLISHED)).thenReturn(null);

            Map<String, Object> stats = postService.platformStats();

            assertThat(stats).containsEntry("totalViews", 0L);
        }
    }

    @Nested
    @DisplayName("Premium and Admin Tests")
    /* This class groups premium admin tests behavior so the module keeps a clear responsibility. */
    class PremiumAdminTests {
        @Test
        @DisplayName("Should apply premium lock to non-pro user")
        void getBySlugPremiumLocked() {
            Post premiumPost = Post.builder()
                .postId(postId)
                .authorId(authorId)
                .title("Test Post Title")
                .slug("test-post-title")
                .content("Premium Content")
                .excerpt("Short excerpt")
                .status(PostStatus.PUBLISHED)
                .visibility(com.inkwell.post.enumtype.PostVisibility.PREMIUM)
                .build();
                
            when(postRepository.findBySlug("test-post-title")).thenReturn(Optional.of(premiumPost));
            when(postRepository.save(any(Post.class))).thenReturn(premiumPost);
            
            GatewayUserPrincipal freeUser = new GatewayUserPrincipal(
                    UUID.randomUUID().toString(), "free", "READER", "free@inkwell.com", "FREE", null
            );
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(freeUser, null, java.util.List.of())
            );

            PostResponse result = postService.getBySlug("test-post-title");

            assertThat(result.content()).contains("Premium Content"); // Content has the lock appended
            assertThat(result.excerpt()).isEqualTo("Short excerpt"); // Excerpt remains the same
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should hide unpublished post from anonymous viewer")
        void getBySlugUnpublishedHiddenFromAnonymous() {
            testPost.setStatus(PostStatus.DRAFT);
            when(postRepository.findBySlug("test-post-title")).thenReturn(Optional.of(testPost));

            assertThatThrownBy(() -> postService.getBySlug("test-post-title"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should allow admin to read another author's draft")
        void getBySlugDraftAllowedForAdmin() {
            testPost.setStatus(PostStatus.DRAFT);
            GatewayUserPrincipal admin = new GatewayUserPrincipal(UUID.randomUUID().toString(), "admin", "admin@test.com", "ADMIN", "PRO", "ACTIVE");
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(admin, null, java.util.List.of())
            );
            when(postRepository.findBySlug("test-post-title")).thenReturn(Optional.of(testPost));

            assertThat(postService.getBySlug("test-post-title").title()).isEqualTo("Test Post Title");
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
        
        @Test
        @DisplayName("Should allow pro user to read premium content")
        void getBySlugPremiumUnlocked() {
            Post premiumPost = Post.builder()
                .postId(postId)
                .authorId(authorId)
                .title("Test Post Title")
                .slug("test-post-title")
                .content("Premium Content")
                .excerpt("Short excerpt")
                .status(PostStatus.PUBLISHED)
                .visibility(com.inkwell.post.enumtype.PostVisibility.PREMIUM)
                .build();
                
            when(postRepository.findBySlug("test-post-title")).thenReturn(Optional.of(premiumPost));
            when(postRepository.save(any(Post.class))).thenReturn(premiumPost);
            
            GatewayUserPrincipal proUser = new GatewayUserPrincipal(
                    UUID.randomUUID().toString(), "pro", "READER", "pro@inkwell.com", "PRO", "ACTIVE"
            );
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(proUser, null, java.util.List.of())
            );

            PostResponse result = postService.getBySlug("test-post-title");

            assertThat(result.content()).isEqualTo("Premium Content");
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Should get by ID successfully")
        void getById() {
            when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
            PostResponse response = postService.getById(postId, authorPrincipal);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Should admin delete post")
        void adminDeletePost() {
            when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));
            
            postService.adminDeletePost(postId);
            
            verify(postLikeRepository).deleteByPostId(postId);
            verify(bookmarkRepository).deleteByPostId(postId);
            verify(postHistoryRepository).deleteByPostId(postId);
            verify(postRepository).delete(testPost);
            verify(rabbitTemplate).convertAndSend(eq("inkwell.exchange"), eq("post.deleted"), any(Map.class));
        }

        @Test
        @DisplayName("Should get post meta")
        void getMeta() {
            when(postRepository.findById(postId)).thenReturn(Optional.of(testPost));

            PostResponse response = postService.getMeta(postId);

            assertThat(response.postId()).isEqualTo(postId);
        }

        @Test
        @DisplayName("Should throw when meta post is missing")
        void getMetaNotFound() {
            when(postRepository.findById(postId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postService.getMeta(postId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
        }
}
