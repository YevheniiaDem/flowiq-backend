package com.flowiq.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feature flags for gradual rollout. Bank integrations are architecturally
 * prepared but disabled until Phase 2+ (see docs/roadmap/BANK_INTEGRATIONS_ROADMAP.md).
 */
@ConfigurationProperties(prefix = "flowiq.features")
public record FeatureFlags(boolean bankIntegrationsEnabled) {
}
