package com.flowiq.unit.audit;

import com.flowiq.audit.support.AuditMetadataSanitizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditMetadataSanitizer unit tests")
class AuditMetadataSanitizerTest {

    @Test
    @DisplayName("removes sensitive keys from metadata")
    void sanitize_removesSensitiveKeys() {
        Map<String, Object> metadata = Map.of(
                "email", "user@test.flowiq",
                "password", "secret123",
                "token", "jwt-value",
                "refreshToken", "refresh-value",
                "fileContent", "csv,data,here"
        );

        Map<String, Object> sanitized = AuditMetadataSanitizer.sanitize(metadata);

        assertThat(sanitized).containsKey("email");
        assertThat(sanitized).doesNotContainKeys("password", "token", "refreshToken", "fileContent");
    }

    @Test
    @DisplayName("sha256 produces stable hash")
    void sha256_producesHexHash() {
        String hash = AuditMetadataSanitizer.sha256("test message");

        assertThat(hash).hasSize(64);
        assertThat(AuditMetadataSanitizer.sha256("test message")).isEqualTo(hash);
    }
}
