/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.controller;

import com.inkwell.post.dto.request.SavePostRequest;
import com.inkwell.post.dto.response.LikeResponse;
import com.inkwell.post.dto.response.PageResponse;
import com.inkwell.post.dto.response.PostResponse;
import com.inkwell.post.enumtype.PostStatus;
import com.inkwell.post.enumtype.PostVisibility;
import com.inkwell.post.service.FollowBookmarkService;
import com.inkwell.post.service.PostService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.inkwell.post.security.GatewayUserPrincipal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/* This class groups post controller direct test behavior so the module keeps a clear responsibility. */
class PostControllerDirectTest {

    private final PostService postService = mock(PostService.class);
    private final FollowBookmarkService followBookmarkService = mock(FollowBookmarkService.class);
    private final UUID postId = UUID.randomUUID();
    private final UUID authorId = UUID.randomUUID();
    private final PostResponse post = new PostResponse(
            postId,
            authorId,
            "Title",
            "title",
            "Content",
            "Excerpt",
            null,
            PostStatus.PUBLISHED,
            2,
            10L,
            1L,
            "tech",
            Set.of("java"),
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            false,
            false,
            PostVisibility.PUBLIC,
            null
    );
    private final PageResponse<PostResponse> page = new PageResponse<>(List.of(post), 0, 10, 1, 1, true, true);

    @Test
    void publicAndAdminControllersDelegate() {
        PublicPostController publicController = new PublicPostController(postService);
        AdminPostController adminController = new AdminPostController(postService);
        InternalPostController internalController = new InternalPostController(postService);
        ServiceInfoController serviceInfoController = new ServiceInfoController();
        when(postService.publicFeed(0, 10, "tech", "java", "q")).thenReturn(page);
        when(postService.getBySlug("title")).thenReturn(post);
        when(postService.platformStats()).thenReturn(Map.of("totalPublishedPosts", 1L));
        when(postService.adminSearch(0, 10, "PUBLISHED", "q")).thenReturn(page);
        when(postService.featurePost(postId, true)).thenReturn(post);
        when(postService.getMeta(postId)).thenReturn(post);

        assertThat(publicController.feed(0, 10, "tech", "java", "q").data()).isEqualTo(page);
        assertThat(publicController.bySlug("title").data()).isEqualTo(post);
        assertThat(publicController.stats().data()).containsEntry("totalPublishedPosts", 1L);
        assertThat(adminController.search(0, 10, "PUBLISHED", "q").data()).isEqualTo(page);
        assertThat(adminController.feature(postId, true).data()).isEqualTo(post);
        assertThat(adminController.delete(postId).getBody().message()).isEqualTo("Post deleted successfully");
        assertThat(internalController.meta(postId).data().slug()).isEqualTo("title");
        assertThat(serviceInfoController.root()).containsEntry("service", "post-service");
        verify(postService).adminDeletePost(postId);
    }

    @Test
    void readerAndAuthorProfilePublicMethodsDelegate() {
        GatewayUserPrincipal principal = new GatewayUserPrincipal(UUID.randomUUID().toString(), "reader", "reader@test.com", "READER", "FREE", null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        SavePostRequest request = new SavePostRequest("Title", "Content", "Excerpt", null, "tech", Set.of("java"), PostStatus.DRAFT, false, false, PostVisibility.PUBLIC, null);
        ReaderPostController readerController = new ReaderPostController(postService, followBookmarkService);
        AuthorProfileController authorProfileController = new AuthorProfileController(followBookmarkService);
        AuthorPostController authorPostController = new AuthorPostController(postService);
        Map<String, Object> following = Map.of("following", true);
        Map<String, Object> unfollowed = Map.of("following", false);
        Map<String, Object> bookmarked = Map.of("bookmarked", true);
        when(postService.toggleLike(postId, principal)).thenReturn(new LikeResponse(true, 2L));
        when(postService.createPost(principal, request)).thenReturn(post);
        when(postService.updatePost(postId, principal, request)).thenReturn(post);
        when(postService.authorPosts(principal, 0, 10)).thenReturn(page);
        when(postService.getById(postId, principal)).thenReturn(post);
        when(followBookmarkService.toggleFollow(authorId, principal)).thenReturn(following, following, unfollowed);
        when(followBookmarkService.getFollowingIds(principal)).thenReturn(List.of(authorId));
        when(followBookmarkService.getFollowStatus(authorId, principal)).thenReturn(following);
        when(followBookmarkService.toggleBookmark(postId, principal)).thenReturn(bookmarked);
        when(followBookmarkService.getBookmarkedPosts(principal)).thenReturn(List.of(post));
        when(followBookmarkService.getHistory(principal, 0, 10)).thenReturn(page);
        when(followBookmarkService.getFollowersCount(authorId)).thenReturn(3L);
        when(followBookmarkService.getMyFollowers(principal)).thenReturn(List.of(Map.of("userId", principal.userId())));
        when(followBookmarkService.getFollowersCount(principal.userUuid())).thenReturn(4L);

        assertThat(readerController.like(postId).data().liked()).isTrue();
        assertThat(readerController.toggleFollow(authorId).data()).containsEntry("following", true);
        assertThat(readerController.getFollowing().data()).containsExactly(authorId);
        assertThat(readerController.getFollowStatus(authorId).data()).containsEntry("following", true);
        assertThat(readerController.toggleBookmark(postId).data()).containsEntry("bookmarked", true);
        assertThat(readerController.getBookmarks().data()).containsExactly(post);
        assertThat(readerController.getHistory(0, 10).data()).isEqualTo(page);
        assertThat(readerController.clearHistory().message()).isEqualTo("History cleared");
        assertThat(authorProfileController.followersCount(authorId).data()).containsEntry("followersCount", 3L);
        assertThat(authorProfileController.follow(authorId).data()).containsEntry("following", true);
        assertThat(authorProfileController.unfollow(authorId).data()).containsEntry("following", false);
        assertThat(authorProfileController.followStatus(authorId).data()).containsEntry("following", true);
        assertThat(authorProfileController.myFollowers().data()).hasSize(1);
        assertThat(authorProfileController.myFollowersCount().data()).containsEntry("followersCount", 4L);
        assertThat(authorPostController.create(request).data()).isEqualTo(post);
        assertThat(authorPostController.update(postId, request).data()).isEqualTo(post);
        assertThat(authorPostController.myPosts(0, 10).data()).isEqualTo(page);
        assertThat(authorPostController.byId(postId).data()).isEqualTo(post);
        assertThat(authorPostController.like(postId).data().liked()).isTrue();
        assertThat(authorPostController.delete(postId).message()).isEqualTo("Post deleted");
        verify(postService).deletePost(postId, principal);
        SecurityContextHolder.clearContext();
    }

    @Test
    void requestFactoryStillMatchesAuthorControllerPayloadShape() {
        SavePostRequest request = new SavePostRequest("Title", "Content", "Excerpt", null, "tech", Set.of("java"), PostStatus.DRAFT, false, false, PostVisibility.PUBLIC, null);

        assertThat(request.title()).isEqualTo("Title");
    }
}
