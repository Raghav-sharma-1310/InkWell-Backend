/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "post_category_mappings")
/* This class groups post category mapping behavior so the module keeps a clear responsibility. */
public class PostCategoryMapping {
    @Id
    @Column(name = "post_id", nullable = false)
    private UUID postId;
    @Column(name = "category_slug", length = 140)
    private String categorySlug;
    @Column(nullable = false)
    private boolean published;
}
