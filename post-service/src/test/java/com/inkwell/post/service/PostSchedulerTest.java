/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.service;

import com.inkwell.post.entity.Post;
import com.inkwell.post.enumtype.PostStatus;
import com.inkwell.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups post scheduler test behavior so the module keeps a clear responsibility. */
class PostSchedulerTest {

    @Mock private PostRepository postRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private PostScheduler postScheduler;

    @Test
    @DisplayName("Should publish scheduled posts that are due")
    void publishScheduledPosts() {
        Post post = Post.builder()
                .postId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .title("Scheduled Post")
                .slug("scheduled-post")
                .excerpt("Test excerpt")
                .status(PostStatus.DRAFT)
                .scheduledAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(postRepository.findByStatusAndScheduledAtBefore(eq(PostStatus.DRAFT), any(LocalDateTime.class)))
                .thenReturn(List.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);

        postScheduler.publishScheduledPosts();

        verify(postRepository).save(argThat(p -> p.getStatus() == PostStatus.PUBLISHED));
        verify(rabbitTemplate).convertAndSend(eq("inkwell.exchange"), eq("post.published"), anyMap());
    }

    @Test
    @DisplayName("Should do nothing when no scheduled posts are due")
    void noScheduledPosts() {
        when(postRepository.findByStatusAndScheduledAtBefore(eq(PostStatus.DRAFT), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        postScheduler.publishScheduledPosts();

        verify(postRepository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), anyMap());
    }

    @Test
    @DisplayName("Should handle RabbitMQ failure gracefully")
    void rabbitMqFailure() {
        Post post = Post.builder()
                .postId(UUID.randomUUID())
                .authorId(UUID.randomUUID())
                .title("Post")
                .slug("post")
                .excerpt("excerpt")
                .status(PostStatus.DRAFT)
                .build();

        when(postRepository.findByStatusAndScheduledAtBefore(eq(PostStatus.DRAFT), any(LocalDateTime.class)))
                .thenReturn(List.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        doThrow(new RuntimeException("RabbitMQ down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyMap());

        postScheduler.publishScheduledPosts();

        verify(postRepository).save(any(Post.class));
    }
}
