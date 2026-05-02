/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@IdClass(PostTagMapping.PostTagMappingId.class)
@Table(name = "post_tag_mappings")
/* This class groups post tag mapping behavior so the module keeps a clear responsibility. */
public class PostTagMapping {
    @Id
    @Column(name = "post_id", nullable = false)
    private UUID postId;
    @Id
    @Column(name = "tag_slug", nullable = false)
    private String tagSlug;
    @Column(nullable = false)
    private boolean published;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    /* This class groups post tag mapping id behavior so the module keeps a clear responsibility. */
    public static class PostTagMappingId implements Serializable {
        private UUID postId;
        private String tagSlug;
    }
}
