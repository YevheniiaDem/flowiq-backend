package com.flowiq.unit.audit;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.AuditOutcome;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.config.AuditProperties;
import com.flowiq.audit.dto.AuditEventRequest;
import com.flowiq.audit.service.AuditLogAsyncWriter;
import com.flowiq.audit.service.AuditLogPersistence;
import com.flowiq.audit.service.AuditServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService unit tests")
class AuditServiceTest {

    @Mock
    private AuditLogPersistence auditLogPersistence;
    @Mock
    private AuditLogAsyncWriter auditLogAsyncWriter;

    private AuditProperties auditProperties;
    private AuditServiceImpl auditService;

    @BeforeEach
    void setUp() {
        auditProperties = new AuditProperties();
        auditProperties.setEnabled(true);
        auditProperties.setAsync(false);
        auditService = new AuditServiceImpl(auditLogPersistence, auditLogAsyncWriter, auditProperties);
    }

    @Test
    @DisplayName("persist delegates to AuditLogPersistence when sync")
    void persist_savesAuditLog() {
        auditService.log(AuditEventRequest.builder()
                .actorUserId(1L)
                .actorEmail("user@test.flowiq")
                .actorRole("USER")
                .eventType(AuditEventType.TRANSACTION_CREATE)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(ResourceType.TRANSACTION)
                .resourceId(42L)
                .metadata(Map.of("amount", 100))
                .build());

        verify(auditLogPersistence).persist(any(AuditEventRequest.class));
        verify(auditLogAsyncWriter, never()).persist(any());
    }

    @Test
    @DisplayName("log is no-op when audit disabled")
    void log_skipsWhenDisabled() {
        auditProperties.setEnabled(false);

        auditService.log(AuditEventRequest.builder()
                .eventType(AuditEventType.AUTH_LOGIN_SUCCESS)
                .outcome(AuditOutcome.SUCCESS)
                .build());

        verify(auditLogPersistence, never()).persist(any());
        verify(auditLogAsyncWriter, never()).persist(any());
    }

    @Test
    @DisplayName("log delegates to async writer when async enabled")
    void log_usesAsyncWriter() {
        auditProperties.setAsync(true);

        auditService.log(AuditEventRequest.builder()
                .eventType(AuditEventType.AUTH_LOGIN_SUCCESS)
                .outcome(AuditOutcome.SUCCESS)
                .build());

        verify(auditLogAsyncWriter).persist(any(AuditEventRequest.class));
        verify(auditLogPersistence, never()).persist(any());
    }

    @Test
    @DisplayName("logSuccess builds success audit event")
    void logSuccess() {
        auditService.logSuccess(
                AuditEventType.TRANSACTION_CREATE,
                ResourceType.TRANSACTION,
                99L,
                Map.of("amount", 500));

        verify(auditLogPersistence).persist(any(AuditEventRequest.class));
    }

    @Test
    @DisplayName("logFailure builds failure audit event")
    void logFailure() {
        auditService.logFailure(
                AuditEventType.AUTH_LOGIN_FAILURE,
                AuditOutcome.FAILURE,
                Map.of("reason", "bad password"));

        verify(auditLogPersistence).persist(any(AuditEventRequest.class));
    }
}
