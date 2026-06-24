package com.flowiq.audit.service;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.AuditOutcome;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.config.AuditProperties;
import com.flowiq.audit.dto.AuditEventRequest;
import com.flowiq.audit.entity.AuditLog;
import com.flowiq.audit.repository.AuditLogRepository;
import com.flowiq.audit.support.AuditContextExtractor;
import com.flowiq.audit.support.AuditMetadataSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditProperties auditProperties;

    @Override
    public void log(AuditEventRequest event) {
        if (!auditProperties.isEnabled()) {
            return;
        }
        if (auditProperties.isAsync()) {
            persistAsync(event);
        } else {
            persist(event);
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

    @Async("auditTaskExecutor")
    public void persistAsync(AuditEventRequest event) {
        persist(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(AuditEventRequest event) {
        try {
            AuditEventRequest.AuditEventRequestBuilder builder = AuditEventRequest.builder()
                    .actorUserId(event.getActorUserId())
                    .actorEmail(event.getActorEmail())
                    .actorRole(event.getActorRole())
                    .eventType(event.getEventType())
                    .outcome(event.getOutcome())
                    .httpMethod(event.getHttpMethod())
                    .requestPath(event.getRequestPath())
                    .resourceType(event.getResourceType())
                    .resourceId(event.getResourceId())
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .correlationId(event.getCorrelationId())
                    .metadata(event.getMetadata());

            AuditContextExtractor.enrich(builder);
            AuditEventRequest enriched = builder.build();

            AuditLog auditLog = new AuditLog();
            auditLog.setActorUserId(enriched.getActorUserId());
            auditLog.setActorEmail(enriched.getActorEmail());
            auditLog.setActorRole(enriched.getActorRole());
            auditLog.setEventType(enriched.getEventType());
            auditLog.setOutcome(enriched.getOutcome());
            auditLog.setHttpMethod(enriched.getHttpMethod());
            auditLog.setRequestPath(enriched.getRequestPath());
            auditLog.setResourceType(enriched.getResourceType() == ResourceType.NONE
                    ? null : enriched.getResourceType());
            auditLog.setResourceId(enriched.getResourceId());
            auditLog.setIpAddress(enriched.getIpAddress());
            auditLog.setUserAgent(enriched.getUserAgent());
            auditLog.setCorrelationId(enriched.getCorrelationId());
            auditLog.setMetadata(AuditMetadataSanitizer.sanitize(enriched.getMetadata()));

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to persist audit log for event {}: {}",
                    event.getEventType(), e.getMessage(), e);
        }
    }
}
