/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.dto.response;

import com.inkwell.newsletter.enumtype.SubscriberStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/* This record groups subscriber response behavior so the module keeps a clear responsibility. */
public record SubscriberResponse(UUID subscriberId, String email, UUID userId, String fullName, SubscriberStatus status, LocalDateTime subscribedAt, LocalDateTime unsubscribedAt, String preferences) {}
