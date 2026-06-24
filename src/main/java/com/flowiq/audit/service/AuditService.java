package com.flowiq.audit.service;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.AuditOutcome;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.dto.AuditEventRequest;

import java.util.Map;

public interface AuditService {

    void log(AuditEventRequest event);

    void logSuccess(AuditEventType type, ResourceType resourceType, Long resourceId,
                    Map<String, Object> metadata);

    void logFailure(AuditEventType type, AuditOutcome outcome, Map<String, Object> metadata);
}
