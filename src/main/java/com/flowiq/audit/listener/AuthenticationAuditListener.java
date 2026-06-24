package com.flowiq.audit.listener;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.AuditOutcome;
import com.flowiq.audit.config.AuditProperties;
import com.flowiq.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthenticationAuditListener {

    private final AuditService auditService;
    private final AuditProperties auditProperties;

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        if (!auditProperties.isEnabled()) {
            return;
        }

        String email = event.getAuthentication().getName();
        auditService.logFailure(
                AuditEventType.AUTH_LOGIN_FAILURE,
                AuditOutcome.FAILURE,
                Map.of("email", email != null ? email.toLowerCase() : "unknown")
        );
    }
}
