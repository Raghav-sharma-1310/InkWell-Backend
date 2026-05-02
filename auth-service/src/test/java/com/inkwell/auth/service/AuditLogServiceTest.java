/*
 * This source file contains business workflow and validation logic for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.auth.service;

import com.inkwell.auth.entity.AuditLog;
import com.inkwell.auth.repository.AuditLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
/* This class groups audit log service test behavior so the module keeps a clear responsibility. */
class AuditLogServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private AuditLogService auditLogService;

    @Test
    @DisplayName("Should log action with all fields")
    void logAction() {
        UUID actorId = UUID.randomUUID();
        auditLogService.logAction(actorId, "admin@inkwell.com", "USER_DELETED", "USER", "123", "Deleted user");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getActorId()).isEqualTo(actorId);
        assertThat(saved.getActorEmail()).isEqualTo("admin@inkwell.com");
        assertThat(saved.getAction()).isEqualTo("USER_DELETED");
        assertThat(saved.getEntityType()).isEqualTo("USER");
        assertThat(saved.getEntityId()).isEqualTo("123");
        assertThat(saved.getDetails()).isEqualTo("Deleted user");
    }

    @Test
    @DisplayName("Should log action with null actorId")
    void logActionNullActor() {
        auditLogService.logAction(null, "System/Admin", "ROLE_CHANGED", "USER", "456", "Changed role");
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should get audit logs with pagination")
    void getAuditLogs() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<AuditLog> page = new PageImpl<>(List.of(AuditLog.builder().build()));
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

        Page<AuditLog> result = auditLogService.getAuditLogs(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findAllByOrderByCreatedAtDesc(pageable);
    }
}
