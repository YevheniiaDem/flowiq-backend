package com.flowiq.unit.config;

import com.flowiq.config.CorsConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CorsConfig tests")
class CorsConfigTest {

    @Test
    @DisplayName("allows frontend origins and required headers")
    void corsConfiguration_allowsFrontend() {
        CorsConfig config = new CorsConfig();
        CorsConfigurationSource source = config.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/transactions");
        CorsConfiguration cors = source.getCorsConfiguration(request);

        assertThat(cors.getAllowedOrigins()).contains("http://localhost:3000");
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(cors.getAllowedHeaders()).contains("Authorization", "Content-Type", "X-App-Language");
        assertThat(cors.getAllowCredentials()).isTrue();
    }
}
