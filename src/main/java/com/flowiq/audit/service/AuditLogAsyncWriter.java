package com.flowiq.audit.service;

import com.flowiq.audit.dto.AuditEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogAsyncWriter {

    private final AuditLogPersistence auditLogPersistence;

    @Async("auditTaskExecutor")
    public void persist(AuditEventRequest event) {
        auditLogPersistence.persist(event);
    }
}
