/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.entity.AuditLog;
import com.inkwell.auth.repository.AuditLogRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
/* This class groups audit log service behavior so the module keeps a clear responsibility. */
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    // Defines log action so related behavior stays grouped in one place.
    public void logAction(UUID actorId, String actorEmail, String action, String entityType, String entityId, String details) {
        AuditLog log = AuditLog.builder()
            .actorId(actorId)
            .actorEmail(actorEmail)
            .action(action)
            .entityType(entityType)
            .entityId(entityId)
            .details(details)
            .build();
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    // Performs the get audit logs workflow so callers do not duplicate this logic.
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
