/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.repository;

import com.inkwell.notification.entity.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups notification repository behavior so the module keeps a clear responsibility. */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId);
    long countByRecipientIdAndReadFalse(UUID recipientId);
    void deleteByRecipientIdAndReadTrue(UUID recipientId);
    
    void deleteByRelatedIdAndRelatedType(String relatedId, String relatedType);
    void deleteByRelatedType(String relatedType);
}
