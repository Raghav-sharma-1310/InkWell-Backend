/*
 * This source file contains persistent domain data for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.entity;

import com.inkwell.newsletter.enumtype.SubscriberStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscribers")
/* This class groups subscriber behavior so the module keeps a clear responsibility. */
public class Subscriber {

    @Id
    @Column(name = "subscriber_id", nullable = false, updatable = false)
    private UUID subscriberId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "full_name")
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriberStatus status;

    @Column(name = "subscribed_at")
    private LocalDateTime subscribedAt;

    @Column(name = "unsubscribed_at")
    private LocalDateTime unsubscribedAt;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(length = 255)
    private String preferences;

    @PrePersist
    void onCreate() {
        if (subscriberId == null) {
            subscriberId = UUID.randomUUID();
        }
        if (subscribedAt == null) {
            subscribedAt = LocalDateTime.now();
        }
    }
}
