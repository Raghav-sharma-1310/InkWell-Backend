/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.post.entity;

import com.inkwell.post.enumtype.PostStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts")
/* This class groups post behavior so the module keeps a clear responsibility. */
public class Post {

    @Id
    @Column(name = "post_id", nullable = false, updatable = false)
    private UUID postId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 500)
    private String excerpt;

    @Column(name = "featured_image_url", length = 500)
    private String featuredImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Convert(converter = com.inkwell.post.config.PostVisibilityConverter.class)
    @Column(nullable = false, length = 20)
    private com.inkwell.post.enumtype.PostVisibility visibility = com.inkwell.post.enumtype.PostVisibility.PUBLIC;

    @Column(name = "read_time_min", nullable = false)
    private Integer readTimeMin;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "likes_count", nullable = false)
    private Long likesCount;

    @Column(name = "category_slug", length = 120)
    private String categorySlug;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag_slug")
    private Set<String> tagSlugs = new LinkedHashSet<>();

    @Column(name = "is_featured", nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean pinned;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @PrePersist
    void onCreate() {
        if (postId == null) {
            postId = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (viewCount == null) {
            viewCount = 0L;
        }
        if (likesCount == null) {
            likesCount = 0L;
        }
        if (readTimeMin == null) {
            readTimeMin = 1;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
