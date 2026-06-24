package com.flowiq.audit.dto;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.AuditOutcome;
import com.flowiq.audit.ResourceType;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AuditEventRequest {

    private Long actorUserId;
    private String actorEmail;
    private String actorRole;
    private AuditEventType eventType;
    private AuditOutcome outcome;
    private String httpMethod;
    private String requestPath;
    private ResourceType resourceType;
    private Long resourceId;
    private String ipAddress;
    private String userAgent;
    private String correlationId;
    private Map<String, Object> metadata;
}
