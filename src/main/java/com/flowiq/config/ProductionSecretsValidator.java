package com.flowiq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Fail-fast when the {@code prod} profile is active but weak/default secrets are still configured.
 */
@Component
@Profile("prod")
@Slf4j
public class ProductionSecretsValidator implements ApplicationListener<ApplicationReadyEvent> {

    private static final Set<String> KNOWN_WEAK_JWT_SECRETS = Set.of(
            "flowiq-dev-secret-key-change-in-production-min-256-bits-long!!",
            "flowiq-test-secret-key-change-in-production-min-256-bits-long!!"
    );

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (jwtSecret == null || jwtSecret.isBlank() || KNOWN_WEAK_JWT_SECRETS.contains(jwtSecret)) {
            throw new IllegalStateException(
                    "Refusing to start with prod profile: set JWT_SECRET to a strong, unique value "
                            + "(minimum 32 random bytes). Do not use committed dev/test defaults.");
        }
        log.info("Production JWT secret validation passed");
    }
}
