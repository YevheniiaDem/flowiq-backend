package com.flowiq.audit.filter;

import com.flowiq.audit.support.AuditContextExtractor;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(AuditContextExtractor.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        request.setAttribute(AuditContextExtractor.CORRELATION_ID_MDC_KEY, correlationId);
        response.setHeader(AuditContextExtractor.CORRELATION_ID_HEADER, correlationId);
        MDC.put(AuditContextExtractor.CORRELATION_ID_MDC_KEY, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(AuditContextExtractor.CORRELATION_ID_MDC_KEY);
        }
    }
}
