/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.category.entity;

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
@Table(name = "categories")
/* This class groups category behavior so the module keeps a clear responsibility. */
public class Category {
    @Id
    @Column(name = "category_id", nullable = false, updatable = false)
    private UUID categoryId;
    @Column(nullable = false, unique = true, length = 120)
    private String name;
    @Column(nullable = false, unique = true, length = 140)
    private String slug;
    @Column(length = 500)
    private String description;
    @Column(name = "parent_category_id")
    private UUID parentCategoryId;
    @Column(name = "post_count", nullable = false)
    private long postCount;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    void onCreate() { if (categoryId == null) { categoryId = UUID.randomUUID(); } createdAt = LocalDateTime.now(); }
}
