/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.notification.repository;

import com.inkwell.notification.entity.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups audit log repository behavior so the module keeps a clear responsibility. */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
