/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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
@Table(name = "media_files")
/* This class groups media file behavior so the module keeps a clear responsibility. */
public class MediaFile {
    @Id
    @Column(name = "media_id", nullable = false, updatable = false)
    private UUID mediaId;
    @Column(name = "uploader_id", nullable = false)
    private UUID uploaderId;
    @Column(nullable = false, length = 180)
    private String filename;
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;
    @Column(nullable = false, length = 500)
    private String url;
    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;
    @Column(name = "size_kb", nullable = false)
    private long sizeKb;
    @Column(name = "alt_text", length = 255)
    private String altText;
    @Column(name = "linked_post_id")
    private UUID linkedPostId;
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;
    @PrePersist
    void onCreate() { if (mediaId == null) { mediaId = UUID.randomUUID(); } uploadedAt = LocalDateTime.now(); }
}
