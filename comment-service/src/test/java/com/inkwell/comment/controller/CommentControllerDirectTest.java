/*
 * This source file contains HTTP controller endpoints for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.comment.controller;

import com.inkwell.comment.dto.request.CommentRequest;
import com.inkwell.comment.dto.request.UpdateCommentRequest;
import com.inkwell.comment.dto.response.CommentResponse;
import com.inkwell.comment.dto.response.LikeResponse;
import com.inkwell.comment.enumtype.CommentStatus;
import com.inkwell.comment.security.GatewayUserPrincipal;
import com.inkwell.comment.service.CommentService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/* This class groups comment controller direct test behavior so the module keeps a clear responsibility. */
class CommentControllerDirectTest {

    private final CommentService commentService = mock(CommentService.class);
    private final CommentController controller = new CommentController(commentService);
    private final UUID commentId = UUID.randomUUID();
    private final UUID postId = UUID.randomUUID();
    private final GatewayUserPrincipal principal = new GatewayUserPrincipal(UUID.randomUUID().toString(), "reader", "reader@test.com", "READER");
    private final CommentResponse comment = new CommentResponse(commentId, postId, principal.userUuid(), "Reader", null, "Nice", 1L, CommentStatus.APPROVED, LocalDateTime.now(), LocalDateTime.now(), false);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void delegatesAllCommentEndpoints() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
        CommentRequest request = new CommentRequest(postId, null, "Nice");
        UpdateCommentRequest updateRequest = new UpdateCommentRequest("Updated");
        when(commentService.byPost(postId)).thenReturn(List.of(comment));
        when(commentService.addComment(principal, request)).thenReturn(comment);
        when(commentService.updateOwn(commentId, principal, updateRequest)).thenReturn(comment);
        when(commentService.toggleLike(commentId, principal)).thenReturn(new LikeResponse(true, 2L));
        when(commentService.replyToComment(commentId, "Reply", principal)).thenReturn(comment);
        when(commentService.moderate(commentId, postId, principal, CommentStatus.APPROVED)).thenReturn(comment);
        when(commentService.moderate(commentId, postId, principal, CommentStatus.REJECTED)).thenReturn(comment);
        when(commentService.moderate(commentId, postId, principal, CommentStatus.DELETED)).thenReturn(comment);
        when(commentService.countAll()).thenReturn(5L);

        assertThat(controller.byPost(postId).data()).containsExactly(comment);
        assertThat(controller.add(request).data()).isEqualTo(comment);
        assertThat(controller.update(commentId, updateRequest).data()).isEqualTo(comment);
        assertThat(controller.delete(commentId).message()).isEqualTo("Comment deleted");
        assertThat(controller.like(commentId).data().liked()).isTrue();
        assertThat(controller.reply(commentId, Map.of("content", "Reply")).data()).isEqualTo(comment);
        assertThat(controller.approve(commentId, postId).data()).isEqualTo(comment);
        assertThat(controller.reject(commentId, postId).data()).isEqualTo(comment);
        assertThat(controller.adminDelete(commentId, postId).data()).isEqualTo(comment);
        assertThat(controller.countAll().data()).isEqualTo(5L);
        assertThat(new ServiceInfoController().root()).containsEntry("service", "comment-service");
        verify(commentService).deleteOwn(commentId, principal);
    }
}
