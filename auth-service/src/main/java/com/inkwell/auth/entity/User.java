/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.entity;

import com.inkwell.auth.enumtype.AuthProvider;
import com.inkwell.auth.enumtype.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "users")
/* This class groups user behavior so the module keeps a clear responsibility. */
public class User {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 80)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(length = 1000)
    private String bio;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false, length = 20)
    private com.inkwell.auth.enumtype.SubscriptionTier subscriptionTier = com.inkwell.auth.enumtype.SubscriptionTier.FREE;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status", length = 20)
    private com.inkwell.auth.enumtype.SubscriptionStatus subscriptionStatus;

    @Column(name = "subscription_end_date")
    private LocalDateTime subscriptionEndDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (userId == null) {
            userId = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
