package com.flowiq.audit.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "flowiq.audit")
public class AuditProperties {

    private boolean enabled = true;
    private boolean async = true;
    private boolean notificationEventsEnabled = false;
    private int retentionFinancialYears = 7;
    private int retentionSecurityDays = 730;
    private int retentionShortDays = 90;
    private boolean purgeEnabled = false;
}
