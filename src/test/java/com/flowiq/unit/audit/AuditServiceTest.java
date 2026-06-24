package com.flowiq.unit.audit;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.AuditOutcome;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.config.AuditProperties;
import com.flowiq.audit.dto.AuditEventRequest;
import com.flowiq.audit.entity.AuditLog;
import com.flowiq.audit.repository.AuditLogRepository;
import com.flowiq.audit.service.AuditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService unit tests")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditProperties auditProperties;
    private AuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        auditProperties = new AuditProperties();
        auditProperties.setEnabled(true);
        auditProperties.setAsync(false);
        auditService = new AuditServiceImpl(auditLogRepository, auditProperties);
    }

    @Test
    @DisplayName("persist saves sanitized audit log entry")
    void persist_savesAuditLog() {
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditService.log(AuditEventRequest.builder()
                .actorUserId(1L)
                .actorEmail("user@test.flowiq")
                .actorRole("USER")
                .eventType(AuditEventType.TRANSACTION_CREATE)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(ResourceType.TRANSACTION)
                .resourceId(42L)
                .metadata(Map.of("amount", 100, "password", "must-not-store"))
                .build());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo(AuditEventType.TRANSACTION_CREATE);
        assertThat(saved.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(saved.getResourceId()).isEqualTo(42L);
        assertThat(saved.getMetadata()).containsEntry("amount", 100);
        assertThat(saved.getMetadata()).doesNotContainKey("password");
    }

    @Test
    @DisplayName("log is no-op when audit disabled")
    void log_skipsWhenDisabled() {
        auditProperties.setEnabled(false);

        auditService.log(AuditEventRequest.builder()
                .eventType(AuditEventType.AUTH_LOGIN_SUCCESS)
                .outcome(AuditOutcome.SUCCESS)
                .build());

        verify(auditLogRepository, never()).save(any());
    }
}
