package com.flowiq.audit.service;

import com.flowiq.audit.dto.AuditEventRequest;
import com.flowiq.audit.entity.AuditLog;
import com.flowiq.audit.repository.AuditLogRepository;
import com.flowiq.audit.support.AuditContextExtractor;
import com.flowiq.audit.support.AuditMetadataSanitizer;
import com.flowiq.audit.ResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogPersistence {

    private final AuditLogRepository auditLogRepository;

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
