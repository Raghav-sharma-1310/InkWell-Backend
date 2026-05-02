/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.service;

import com.inkwell.comment.client.PostClient;
import com.inkwell.comment.dto.ApiResponse;
import com.inkwell.comment.dto.request.CommentRequest;
import com.inkwell.comment.dto.request.UpdateCommentRequest;
import com.inkwell.comment.dto.response.CommentResponse;
import com.inkwell.comment.dto.response.LikeResponse;
import com.inkwell.comment.dto.response.PostMetaResponse;
import com.inkwell.comment.entity.Comment;
import com.inkwell.comment.entity.CommentLike;
import com.inkwell.comment.enumtype.CommentStatus;
import com.inkwell.comment.exception.ForbiddenException;
import com.inkwell.comment.exception.ResourceNotFoundException;
import com.inkwell.comment.repository.CommentLikeRepository;
import com.inkwell.comment.repository.CommentRepository;
import com.inkwell.comment.security.GatewayUserPrincipal;
import com.inkwell.comment.util.HtmlSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups comment service test behavior so the module keeps a clear responsibility. */
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private CommentLikeRepository commentLikeRepository;
    @Mock private HtmlSanitizer htmlSanitizer;
    @Mock private PostClient postClient;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private CommentService commentService;

    private UUID postId, authorId, commentId;
    private GatewayUserPrincipal principal;
    private PostMetaResponse postMeta;

    @BeforeEach
    void setUp() {
        postId = UUID.randomUUID();
        authorId = UUID.randomUUID();
        commentId = UUID.randomUUID();
        principal = new GatewayUserPrincipal(authorId.toString(), "testuser", "test@inkwell.com", "READER");
        postMeta = new PostMetaResponse(postId, UUID.randomUUID(), "Post Title", "post-slug");
    }

    @Test
    @DisplayName("Should list comments by post")
    void byPost() {
        Comment comment = Comment.builder().commentId(commentId).postId(postId).authorId(authorId).authorName("testuser").content("Hello").likesCount(0L).status(CommentStatus.APPROVED).build();
        when(postClient.getMeta(postId)).thenReturn(new ApiResponse<>(java.time.Instant.now(), "success", postMeta));
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(postId)).thenReturn(List.of(comment));

        List<CommentResponse> result = commentService.byPost(postId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("Should list comments by post even if meta fetch fails")
    void byPostMetaFetchFails() {
        Comment comment = Comment.builder().commentId(commentId).postId(postId).authorId(authorId).authorName("testuser").content("Hello").likesCount(0L).status(CommentStatus.APPROVED).build();
        when(postClient.getMeta(postId)).thenThrow(new RuntimeException("Service unavailable"));
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(postId)).thenReturn(List.of(comment));

        List<CommentResponse> result = commentService.byPost(postId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPostAuthor()).isFalse();
    }

    @Test
    @DisplayName("Should add a new comment")
    void addComment() {
        CommentRequest request = new CommentRequest(postId, null, "Great post!");
        when(postClient.getMeta(postId)).thenReturn(new ApiResponse<>(java.time.Instant.now(), "success", postMeta));
        when(htmlSanitizer.sanitize("Great post!")).thenReturn("Great post!");
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setCommentId(commentId);
            return c;
        });

        CommentResponse response = commentService.addComment(principal, request);

        assertThat(response).isNotNull();
        verify(rabbitTemplate).convertAndSend(eq("inkwell.exchange"), eq("comment.created"), anyMap());
    }

    @Test
    @DisplayName("Should add a reply comment")
    void addReplyComment() {
        UUID parentId = UUID.randomUUID();
        CommentRequest request = new CommentRequest(postId, parentId, "Reply!");
        when(postClient.getMeta(postId)).thenReturn(new ApiResponse<>(java.time.Instant.now(), "success", postMeta));
        when(htmlSanitizer.sanitize("Reply!")).thenReturn("Reply!");
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setCommentId(commentId);
            return c;
        });

        CommentResponse response = commentService.addComment(principal, request);

        assertThat(response).isNotNull();
        verify(rabbitTemplate).convertAndSend(eq("inkwell.exchange"), eq("comment.reply"), anyMap());
    }

    @Test
    void authorCanReplyToCommentOnOwnPost() {
        UUID parentId = UUID.randomUUID();
        Comment parent = Comment.builder().commentId(parentId).postId(postId).authorId(UUID.randomUUID())
            .content("Parent").likesCount(0L).status(CommentStatus.APPROVED).build();
        PostMetaResponse ownPost = new PostMetaResponse(postId, authorId, "Post Title", "post-slug");
        when(commentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(postClient.getMeta(postId)).thenReturn(new ApiResponse<>(java.time.Instant.now(), "success", ownPost));
        when(htmlSanitizer.sanitize("Author reply")).thenReturn("Author reply");
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment reply = invocation.getArgument(0);
            reply.setCommentId(commentId);
            return reply;
        });

        CommentResponse response = commentService.replyToComment(parentId, "Author reply", principal);

        assertThat(response.parentCommentId()).isEqualTo(parentId);
        assertThat(response.isPostAuthor()).isTrue();
        verify(rabbitTemplate).convertAndSend(eq("inkwell.exchange"), eq("comment.reply"), anyMap());
    }

    @Test
    void nonAuthorCannotReplyAsAuthor() {
        UUID parentId = UUID.randomUUID();
        Comment parent = Comment.builder().commentId(parentId).postId(postId).authorId(UUID.randomUUID())
            .content("Parent").likesCount(0L).status(CommentStatus.APPROVED).build();
        when(commentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(postClient.getMeta(postId)).thenReturn(new ApiResponse<>(java.time.Instant.now(), "success", postMeta));

        assertThatThrownBy(() -> commentService.replyToComment(parentId, "Nope", principal))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("Only the post author");
    }

    @Test
    @DisplayName("Should update own comment")
    void updateOwn() {
        Comment comment = Comment.builder().commentId(commentId).postId(postId).authorId(authorId).content("Old").likesCount(0L).status(CommentStatus.APPROVED).build();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(htmlSanitizer.sanitize("Updated")).thenReturn("Updated");
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        CommentResponse response = commentService.updateOwn(commentId, principal, new UpdateCommentRequest("Updated"));

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("Should reject editing another user's comment")
    void updateOwnForbidden() {
        Comment comment = Comment.builder().commentId(commentId).postId(postId).authorId(UUID.randomUUID()).content("Old").likesCount(0L).status(CommentStatus.APPROVED).build();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        UpdateCommentRequest request = new UpdateCommentRequest("Updated");
        assertThatThrownBy(() -> commentService.updateOwn(commentId, principal, request))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Should delete own comment")
    void deleteOwn() {
        Comment comment = Comment.builder().commentId(commentId).postId(postId).authorId(authorId).content("Old").likesCount(0L).status(CommentStatus.APPROVED).build();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        commentService.deleteOwn(commentId, principal);

        verify(commentRepository).save(argThat(c -> c.getStatus() == CommentStatus.DELETED));
    }

    @Test
    void adminCanDeleteAnotherUsersCommentButReaderCannot() {
        Comment comment = Comment.builder().commentId(commentId).postId(postId).authorId(UUID.randomUUID())
            .content("Old").likesCount(0L).status(CommentStatus.APPROVED).build();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteOwn(commentId, principal))
            .isInstanceOf(ForbiddenException.class);

        GatewayUserPrincipal admin = new GatewayUserPrincipal(UUID.randomUUID().toString(), "admin", "admin@test.com", "ADMIN");
        commentService.deleteOwn(commentId, admin);

        verify(commentRepository).save(argThat(c -> c.getStatus() == CommentStatus.DELETED
            && c.getContent().equals("Comment removed by user")));
    }

    @Test
    @DisplayName("Should toggle like — add")
    void toggleLikeAdd() {
        Comment comment = Comment.builder().commentId(commentId).postId(postId).authorId(UUID.randomUUID()).content("Hi").likesCount(0L).status(CommentStatus.APPROVED).build();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.existsByCommentIdAndUserId(commentId, authorId)).thenReturn(false);
        when(commentLikeRepository.countByCommentId(commentId)).thenReturn(1L);

        LikeResponse response = commentService.toggleLike(commentId, principal);

        assertThat(response.liked()).isTrue();
        assertThat(response.likesCount()).isEqualTo(1L);
        verify(commentLikeRepository).save(any(CommentLike.class));
    }

    @Test
    @DisplayName("Should toggle like — remove")
    void toggleLikeRemove() {
        Comment comment = Comment.builder().commentId(commentId).postId(postId).authorId(UUID.randomUUID()).content("Hi").likesCount(1L).status(CommentStatus.APPROVED).build();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentLikeRepository.existsByCommentIdAndUserId(commentId, authorId)).thenReturn(true);
        when(commentLikeRepository.countByCommentId(commentId)).thenReturn(0L);

        LikeResponse response = commentService.toggleLike(commentId, principal);

        assertThat(response.liked()).isFalse();
        verify(commentLikeRepository).deleteByCommentIdAndUserId(commentId, authorId);
    }

    @Test
    @DisplayName("Should count comments by post")
    void countByPost() {
        when(commentRepository.countByPostIdAndStatus(postId, CommentStatus.APPROVED)).thenReturn(5L);
        assertThat(commentService.countByPost(postId)).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should count all non-deleted comments")
    void countAll() {
        when(commentRepository.countByStatusNot(CommentStatus.DELETED)).thenReturn(10L);
        assertThat(commentService.countAll()).isEqualTo(10L);
    }

    @Test
    void moderateAllowsAdminAndPostAuthorAndHandlesDeletedStatus() {
        Comment comment = Comment.builder().commentId(commentId).postId(postId).authorId(UUID.randomUUID())
            .content("Needs review").likesCount(0L).status(CommentStatus.APPROVED).build();
        GatewayUserPrincipal admin = new GatewayUserPrincipal(UUID.randomUUID().toString(), "admin", "admin@test.com", "ADMIN");
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(comment)).thenReturn(comment);

        CommentResponse deleted = commentService.moderate(commentId, postId, admin, CommentStatus.DELETED);

        assertThat(deleted.status()).isEqualTo(CommentStatus.DELETED);
        assertThat(comment.getContent()).isEqualTo("Comment removed by moderator");

        comment.setStatus(CommentStatus.APPROVED);
        PostMetaResponse ownPost = new PostMetaResponse(postId, authorId, "Post Title", "post-slug");
        when(postClient.getMeta(postId)).thenReturn(new ApiResponse<>(java.time.Instant.now(), "success", ownPost));

        CommentResponse hidden = commentService.moderate(commentId, postId, principal, CommentStatus.REJECTED);

        assertThat(hidden.status()).isEqualTo(CommentStatus.REJECTED);
    }

    @Test
    void moderateRejectsNonAuthorAndMissingComment() {
        Comment comment = Comment.builder().commentId(commentId).postId(postId).authorId(UUID.randomUUID())
            .content("Needs review").likesCount(0L).status(CommentStatus.APPROVED).build();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(postClient.getMeta(postId)).thenReturn(new ApiResponse<>(java.time.Instant.now(), "success", postMeta));

        assertThatThrownBy(() -> commentService.moderate(commentId, postId, principal, CommentStatus.REJECTED))
            .isInstanceOf(ForbiddenException.class);

        UUID missingId = UUID.randomUUID();
        UpdateCommentRequest updateRequest = new UpdateCommentRequest("x");
        when(commentRepository.findById(missingId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> commentService.updateOwn(missingId, principal, updateRequest))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should handle post deletion event")
    void onPostDeleted() {
        Comment comment = Comment.builder().commentId(commentId).postId(postId).authorId(authorId).content("Old").likesCount(0L).status(CommentStatus.APPROVED).build();
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(postId)).thenReturn(List.of(comment));

        commentService.onPostDeleted(Map.of("postId", postId.toString()));

        verify(commentRepository).save(argThat(c -> c.getStatus() == CommentStatus.DELETED));
    }
}
