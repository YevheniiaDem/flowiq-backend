package com.flowiq.audit.aspect;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.AuditOutcome;
import com.flowiq.audit.ResourceType;
import com.flowiq.audit.config.AuditProperties;
import com.flowiq.audit.dto.AuditEventRequest;
import com.flowiq.audit.service.AuditService;
import com.flowiq.audit.support.AuditMetadataBuilder;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final AuditProperties auditProperties;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        if (!auditProperties.isEnabled()) {
            return joinPoint.proceed();
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            writeAudit(joinPoint, auditable, result, AuditOutcome.SUCCESS, null);
            return result;
        } catch (Throwable throwable) {
            if (auditable.logOnFailure()) {
                writeAudit(joinPoint, auditable, result, mapOutcome(throwable), throwable);
            }
            throw throwable;
        }
    }

    private void writeAudit(ProceedingJoinPoint joinPoint, Auditable auditable, Object result,
                            AuditOutcome outcome, Throwable error) {
        Long resourceId = resolveResourceId(joinPoint, auditable, result);
        Map<String, Object> metadata = AuditMetadataBuilder.build(
                auditable.value(),
                joinPoint.getArgs(),
                result,
                error instanceof Exception exception ? exception : null
        );

        auditService.log(AuditEventRequest.builder()
                .eventType(auditable.value())
                .outcome(outcome)
                .resourceType(auditable.resourceType())
                .resourceId(resourceId)
                .metadata(metadata)
                .build());
    }

    private Long resolveResourceId(ProceedingJoinPoint joinPoint, Auditable auditable, Object result) {
        String expression = auditable.resourceId();
        if (expression == null || expression.isBlank()) {
            return null;
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariable("result", unwrap(result));

        String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        Object value = expressionParser.parseExpression(expression).getValue(context);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return Long.parseLong(stringValue);
        }
        return null;
    }

    private Object unwrap(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getBody();
        }
        return result;
    }

    private AuditOutcome mapOutcome(Throwable throwable) {
        if (throwable instanceof BadRequestException
                || throwable instanceof UnauthorizedException
                || throwable instanceof ResourceNotFoundException) {
            return AuditOutcome.FAILURE;
        }
        return AuditOutcome.ERROR;
    }
}
