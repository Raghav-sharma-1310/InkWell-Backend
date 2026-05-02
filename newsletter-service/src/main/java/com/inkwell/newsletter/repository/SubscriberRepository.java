/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.repository;

import com.inkwell.newsletter.entity.Subscriber;
import com.inkwell.newsletter.enumtype.SubscriberStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups subscriber repository behavior so the module keeps a clear responsibility. */
public interface SubscriberRepository extends JpaRepository<Subscriber, UUID> {
    Optional<Subscriber> findByEmailIgnoreCase(String email);
    Optional<Subscriber> findByToken(String token);
    List<Subscriber> findByStatus(SubscriberStatus status);
    Optional<Subscriber> findByUserId(UUID userId);
}
