package com.flowiq.audit.aspect;

import com.flowiq.audit.AuditEventType;
import com.flowiq.audit.ResourceType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    AuditEventType value();

    ResourceType resourceType() default ResourceType.NONE;

    /** SpEL expression, e.g. #result.id or #id */
    String resourceId() default "";

    boolean logOnFailure() default true;
}
