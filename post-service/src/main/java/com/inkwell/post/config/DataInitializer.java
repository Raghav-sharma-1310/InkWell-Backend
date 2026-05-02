/*
 * This source file contains Spring Boot configuration for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.config;

import com.inkwell.post.entity.Post;
import com.inkwell.post.enumtype.PostStatus;
import com.inkwell.post.repository.PostRepository;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
/* This class groups data initializer behavior so the module keeps a clear responsibility. */
public class DataInitializer implements CommandLineRunner {

    private final PostRepository postRepository;

    @Override
    // Defines run so related behavior stays grouped in one place.
    public void run(String... args) {
        if (postRepository.count() > 0) {
            return;
        }

        postRepository.save(Post.builder()
            .postId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
            .authorId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
            .title("Building InkWell with Spring Microservices")
            .slug("building-inkwell-with-spring-microservices")
            .content("<p>InkWell brings together Spring Boot, Redis, RabbitMQ, and React for a production-style blogging workflow.</p><pre><code>docker compose up --build</code></pre>")
            .excerpt("InkWell brings together Spring Boot, Redis, RabbitMQ, and React for a production-style blogging workflow.")
            .featuredImageUrl("https://images.unsplash.com/photo-1516321318423-f06f85e504b3")
            .status(PostStatus.PUBLISHED)
            .readTimeMin(2)
            .viewCount(120L)
            .likesCount(14L)
            .categorySlug("engineering")
            .tagSlugs(Set.of("spring-boot", "microservices", "react"))
            .featured(true)
            .pinned(true)
            .publishedAt(LocalDateTime.now().minusDays(2))
            .build());
    }
}
