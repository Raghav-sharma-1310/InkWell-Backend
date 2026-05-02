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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
/* This class groups comment service behavior so the module keeps a clear responsibility. */
public class CommentService {

    private static final String KEY_POST_ID = "postId";

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final HtmlSanitizer htmlSanitizer;
    private final PostClient postClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional(readOnly = true)
    // Defines by post so related behavior stays grouped in one place.
    public List<CommentResponse> byPost(UUID postId) {
        UUID postAuthorId = null;
        try {
            PostMetaResponse meta = postClient.getMeta(postId).data();
            postAuthorId = meta != null ? meta.authorId() : null;
        } catch (Exception e) {
            log.warn("Could not fetch post meta for author badge: {}", e.getMessage());
        }
        UUID finalPostAuthorId = postAuthorId;
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
            .map(c -> toResponse(c, finalPostAuthorId))
            .toList();
    }

    @Transactional
    // Defines add comment so related behavior stays grouped in one place.
    public CommentResponse addComment(GatewayUserPrincipal principal, CommentRequest request) {
        PostMetaResponse post = postClient.getMeta(request.postId()).data();
        CommentStatus status = CommentStatus.APPROVED;
        Comment comment = commentRepository.save(Comment.builder()
            .postId(request.postId())
            .authorId(principal.userUuid())
            .authorName(principal.username())
            .parentCommentId(request.parentCommentId())
            .content(htmlSanitizer.sanitize(request.content()))
            .likesCount(0L)
            .status(status)
            .build());
        rabbitTemplate.convertAndSend("inkwell.exchange", request.parentCommentId() == null ? "comment.created" : "comment.reply", Map.of(
            "commentId", comment.getCommentId().toString(),
            KEY_POST_ID, request.postId().toString(),
            "postAuthorId", post.authorId().toString(),
            "commentAuthorId", principal.userUuid().toString(),
            "parentCommentId", String.valueOf(request.parentCommentId())
        ));
        return toResponse(comment, post.authorId());
    }

    /**
     * Author reply to a comment on their own post.
     * Only the post author can use this endpoint.
     */
    @Transactional
    public CommentResponse replyToComment(UUID parentCommentId, String content, GatewayUserPrincipal principal) {
        Comment parentComment = getComment(parentCommentId);
        PostMetaResponse post = postClient.getMeta(parentComment.getPostId()).data();

        // Only the post author can reply as "Author"
        if (!post.authorId().equals(principal.userUuid())) {
            throw new ForbiddenException("Only the post author can reply as Author");
        }

        Comment reply = commentRepository.save(Comment.builder()
            .postId(parentComment.getPostId())
            .authorId(principal.userUuid())
            .authorName(principal.username())
            .parentCommentId(parentCommentId)
            .content(htmlSanitizer.sanitize(content))
            .likesCount(0L)
            .status(CommentStatus.APPROVED)
            .build());

        rabbitTemplate.convertAndSend("inkwell.exchange", "comment.reply", Map.of(
            "commentId", reply.getCommentId().toString(),
            KEY_POST_ID, parentComment.getPostId().toString(),
            "postAuthorId", post.authorId().toString(),
            "commentAuthorId", principal.userUuid().toString(),
            "parentCommentId", parentCommentId.toString()
        ));

        return toResponse(reply, post.authorId());
    }

    @Transactional
    // Performs the update own workflow so callers do not duplicate this logic.
    public CommentResponse updateOwn(UUID commentId, GatewayUserPrincipal principal, UpdateCommentRequest request) {
        Comment comment = getComment(commentId);
        if (!comment.getAuthorId().equals(principal.userUuid())) {
            throw new ForbiddenException("Cannot edit another user's comment");
        }
        comment.setContent(htmlSanitizer.sanitize(request.content()));
        return toResponse(commentRepository.save(comment), null);
    }

    @Transactional
    // Performs the delete own workflow so callers do not duplicate this logic.
    public void deleteOwn(UUID commentId, GatewayUserPrincipal principal) {
        Comment comment = getComment(commentId);
        if (!comment.getAuthorId().equals(principal.userUuid()) && !principal.isAdmin()) {
            throw new ForbiddenException("Cannot delete another user's comment");
        }
        comment.setStatus(CommentStatus.DELETED);
        comment.setContent("Comment removed by user");
        commentRepository.save(comment);
    }

    @Transactional
    // Defines toggle like so related behavior stays grouped in one place.
    public LikeResponse toggleLike(UUID commentId, GatewayUserPrincipal principal) {
        Comment comment = getComment(commentId);
        boolean liked = !commentLikeRepository.existsByCommentIdAndUserId(commentId, principal.userUuid());
        if (liked) {
            commentLikeRepository.save(new CommentLike(commentId, principal.userUuid(), LocalDateTime.now()));
        } else {
            commentLikeRepository.deleteByCommentIdAndUserId(commentId, principal.userUuid());
        }
        comment.setLikesCount(commentLikeRepository.countByCommentId(commentId));
        commentRepository.save(comment);
        return new LikeResponse(liked, comment.getLikesCount());
    }

    @Transactional
    // Defines moderate so related behavior stays grouped in one place.
    public CommentResponse moderate(UUID commentId, UUID postId, GatewayUserPrincipal principal, CommentStatus status) {
        Comment comment = getComment(commentId);
        if (!principal.isAdmin()) {
            PostMetaResponse post = postClient.getMeta(postId).data();
            if (!post.authorId().equals(principal.userUuid())) {
                throw new ForbiddenException("Only the author of the post can moderate this comment");
            }
        }
        comment.setStatus(status);
        if (status == CommentStatus.DELETED) {
            comment.setContent("Comment removed by moderator");
        }
        return toResponse(commentRepository.save(comment), null);
    }

    @Transactional(readOnly = true)
    // Defines count by post so related behavior stays grouped in one place.
    public long countByPost(UUID postId) {
        return commentRepository.countByPostIdAndStatus(postId, CommentStatus.APPROVED);
    }

    @Transactional(readOnly = true)
    // Defines count all so related behavior stays grouped in one place.
    public long countAll() {
        return commentRepository.countByStatusNot(CommentStatus.DELETED);
    }

    @RabbitListener(queues = "post-deleted-queue")
    @Transactional
    // Performs the on post deleted workflow so callers do not duplicate this logic.
    public void onPostDeleted(Map<String, Object> payload) {
        UUID postId = UUID.fromString(String.valueOf(payload.get(KEY_POST_ID)));
        commentRepository.findByPostIdOrderByCreatedAtAsc(postId).forEach(comment -> {
            comment.setStatus(CommentStatus.DELETED);
            comment.setContent("Post removed");
            commentRepository.save(comment);
        });
    }

    // Performs the get comment workflow so callers do not duplicate this logic.
    private Comment getComment(UUID commentId) {
        return commentRepository.findById(commentId).orElseThrow(() -> new ResourceNotFoundException("Comment not found"));
    }

    // Defines to response so related behavior stays grouped in one place.
    private CommentResponse toResponse(Comment comment, UUID postAuthorId) {
        boolean isPostAuthor = postAuthorId != null && postAuthorId.equals(comment.getAuthorId());
        return new CommentResponse(comment.getCommentId(), comment.getPostId(), comment.getAuthorId(),
            comment.getAuthorName(), comment.getParentCommentId(), comment.getContent(),
            comment.getLikesCount(), comment.getStatus(), comment.getCreatedAt(), comment.getUpdatedAt(),
            isPostAuthor);
    }
}
