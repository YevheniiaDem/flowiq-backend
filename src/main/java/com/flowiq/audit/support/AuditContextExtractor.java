package com.flowiq.audit.support;

import com.flowiq.audit.dto.AuditEventRequest;
import com.flowiq.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class AuditContextExtractor {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";

    private AuditContextExtractor() {
    }

    public static void enrich(AuditEventRequest.AuditEventRequestBuilder builder) {
        resolveActor(builder);
        resolveHttpContext(builder);
    }

    private static void resolveActor(AuditEventRequest.AuditEventRequestBuilder builder) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            builder.actorUserId(principal.getId());
            builder.actorEmail(principal.getEmail());
            builder.actorRole(principal.getRole().name());
        }
    }

    private static void resolveHttpContext(AuditEventRequest.AuditEventRequestBuilder builder) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            builder.correlationId(MDC.get(CORRELATION_ID_MDC_KEY));
            return;
        }

        builder.httpMethod(request.getMethod());
        builder.requestPath(request.getRequestURI());
        builder.ipAddress(resolveClientIp(request));
        builder.userAgent(truncate(request.getHeader("User-Agent"), 512));

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = (String) request.getAttribute(CORRELATION_ID_MDC_KEY);
        }
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = MDC.get(CORRELATION_ID_MDC_KEY);
        }
        builder.correlationId(correlationId);
    }

    public static HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }

    public static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
