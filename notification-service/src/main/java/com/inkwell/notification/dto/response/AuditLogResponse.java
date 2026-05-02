/*
 * This source file contains request and response data shapes for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/* This record groups audit log response behavior so the module keeps a clear responsibility. */
public record AuditLogResponse(UUID auditId, UUID actorId, String action, String source, String details, LocalDateTime createdAt) {}
