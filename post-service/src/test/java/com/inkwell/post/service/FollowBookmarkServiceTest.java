/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.service;

import com.inkwell.post.entity.Follow;
import com.inkwell.post.entity.Post;
import com.inkwell.post.entity.Bookmark;
import com.inkwell.post.entity.PostHistory;
import com.inkwell.post.exception.ResourceNotFoundException;
import com.inkwell.post.repository.FollowRepository;
import com.inkwell.post.repository.BookmarkRepository;
import com.inkwell.post.repository.PostHistoryRepository;
import com.inkwell.post.repository.PostRepository;
import com.inkwell.post.security.GatewayUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups follow bookmark service test behavior so the module keeps a clear responsibility. */
class FollowBookmarkServiceTest {

    @Mock
    private FollowRepository followRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PostHistoryRepository postHistoryRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private FollowBookmarkService followBookmarkService;

    private GatewayUserPrincipal proPrincipal;
    private GatewayUserPrincipal freePrincipal;
    private UUID userId;
    private UUID authorId;
    private UUID postId;
    private Post post;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        postId = UUID.randomUUID();
        proPrincipal = new GatewayUserPrincipal(userId.toString(), "user", "READER", "user@test.com", "PRO", "ACTIVE");
        freePrincipal = new GatewayUserPrincipal(userId.toString(), "user", "READER", "user@test.com", "FREE", "INACTIVE");

        post = Post.builder()
                .postId(postId)
                .authorId(authorId)
                .slug("test-slug")
                .build();
    }

    @Test
    void testFollowAuthor() {
        when(followRepository.existsByFollowerIdAndFollowedId(userId, authorId)).thenReturn(false);
        followBookmarkService.toggleFollow(authorId, proPrincipal);
        verify(followRepository).save(any(Follow.class));
    }

    @Test
    void testUnfollowAuthor() {
        when(followRepository.existsByFollowerIdAndFollowedId(userId, authorId)).thenReturn(true);
        followBookmarkService.toggleFollow(authorId, proPrincipal);
        verify(followRepository).deleteByFollowerIdAndFollowedId(userId, authorId);
    }

    @Test
    void testGetFollowersCount() {
        when(followRepository.countByFollowedId(authorId)).thenReturn(10L);
        assertEquals(10L, followBookmarkService.getFollowersCount(authorId));
    }

    @Test
    void testCheckFollowing() {
        when(followRepository.existsByFollowerIdAndFollowedId(userId, authorId)).thenReturn(true);
        assertTrue((Boolean) followBookmarkService.getFollowStatus(authorId, proPrincipal).get("following"));
    }

    @Test
    void testBookmarkPost() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(bookmarkRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);
        followBookmarkService.toggleBookmark(postId, proPrincipal);
        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    void testRemoveBookmark() {
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(bookmarkRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(true);
        followBookmarkService.toggleBookmark(postId, proPrincipal);
        verify(bookmarkRepository).deleteByUserIdAndPostId(userId, postId);
    }

    @Test
    void testGetBookmarks() {
        when(bookmarkRepository.findPostIdsByUserId(userId)).thenReturn(List.of(postId));
        when(postRepository.findAllById(any())).thenReturn(List.of(post));
        
        var result = followBookmarkService.getBookmarkedPosts(proPrincipal);
        assertNotNull(result);
    }

    @Test
    void testRecordHistory() {
        when(postRepository.findBySlug("test-slug")).thenReturn(Optional.of(post));
        when(postHistoryRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());
        
        followBookmarkService.recordHistory("test-slug", proPrincipal);
        verify(postHistoryRepository).save(any(PostHistory.class));
    }

    @Test
    void testRecordHistory_FreeUser_ThrowsForbidden() {
        assertThrows(com.inkwell.post.exception.ForbiddenException.class, () -> followBookmarkService.recordHistory("test-slug", freePrincipal));
    }

    @Test
    void testGetHistory() {
        PostHistory history = PostHistory.builder().postId(postId).build();
        Page<PostHistory> page = new PageImpl<>(List.of(history));
        when(postHistoryRepository.findByUserIdOrderByViewedAtDesc(eq(userId), any(Pageable.class))).thenReturn(page);
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        var result = followBookmarkService.getHistory(proPrincipal, 0, 10);
        assertNotNull(result);
        assertEquals(1, result.totalElements());
    }

    @Test
    void testClearHistory() {
        followBookmarkService.clearHistory(proPrincipal);
        verify(postHistoryRepository).deleteByUserId(userId);
    }

    @Test
    void testDeleteHistoryItem() {
        PostHistory history = PostHistory.builder().postId(postId).userId(userId).build();
        when(postHistoryRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.of(history));

        followBookmarkService.deleteHistoryItem(postId, proPrincipal);
        verify(postHistoryRepository).delete(history);
    }

    @Test
    void testDeleteHistoryItem_NotFound() {
        when(postHistoryRepository.findByUserIdAndPostId(userId, postId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> followBookmarkService.deleteHistoryItem(postId, proPrincipal));
    }
}
