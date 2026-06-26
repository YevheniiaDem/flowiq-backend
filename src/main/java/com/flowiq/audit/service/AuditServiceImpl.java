package com.flowiq.audit.service;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.AuditOutcome;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.config.AuditProperties;
import com.flowiq.audit.dto.AuditEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogPersistence auditLogPersistence;
    private final AuditLogAsyncWriter auditLogAsyncWriter;
    private final AuditProperties auditProperties;

    @Override
    public void log(AuditEventRequest event) {
        if (!auditProperties.isEnabled()) {
            return;
        }
        if (auditProperties.isAsync()) {
            auditLogAsyncWriter.persist(event);
        } else {
            auditLogPersistence.persist(event);
        }
    }

    @Override
    public void logSuccess(AuditEventType type, ResourceType resourceType, Long resourceId,
                           Map<String, Object> metadata) {
        log(AuditEventRequest.builder()
                .eventType(type)
                .outcome(AuditOutcome.SUCCESS)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .metadata(metadata)
                .build());
    }

    @Override
    public void logFailure(AuditEventType type, AuditOutcome outcome, Map<String, Object> metadata) {
        log(AuditEventRequest.builder()
                .eventType(type)
                .outcome(outcome)
                .metadata(metadata)
                .build());
    }
}
