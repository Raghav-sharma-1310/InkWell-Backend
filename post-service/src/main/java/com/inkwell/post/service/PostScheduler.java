/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.service;

import com.inkwell.post.entity.Post;
import com.inkwell.post.enumtype.PostStatus;
import com.inkwell.post.repository.PostRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
/* This class groups post scheduler behavior so the module keeps a clear responsibility. */
public class PostScheduler {

    private final PostRepository postRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional
    // Defines publish scheduled posts so related behavior stays grouped in one place.
    public void publishScheduledPosts() {
        LocalDateTime now = LocalDateTime.now();
        List<Post> duePosts = postRepository.findByStatusAndScheduledAtBefore(PostStatus.DRAFT, now);
        
        if (!duePosts.isEmpty()) {
            log.info("Found {} scheduled posts due for publishing", duePosts.size());
            
            for (Post post : duePosts) {
                post.setStatus(PostStatus.PUBLISHED);
                post.setPublishedAt(now);
                post.setScheduledAt(null);
                postRepository.save(post);
                
                log.info("Published scheduled post: {}", post.getPostId());
                
                try {
                    rabbitTemplate.convertAndSend("inkwell.exchange", "post.published", 
                        Map.of(
                            "postId", post.getPostId().toString(),
                            "authorId", post.getAuthorId().toString(),
                            "title", post.getTitle(),
                            "slug", post.getSlug(),
                            "excerpt", post.getExcerpt() != null ? post.getExcerpt() : "",
                            "categorySlug", String.valueOf(post.getCategorySlug())
                        )
                    );
                } catch (Exception e) {
                    log.error("Failed to send post.published event for post {}", post.getPostId(), e);
                }
            }
        }
    }
}
