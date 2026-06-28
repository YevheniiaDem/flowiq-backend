package com.flowiq.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSecretsValidatorTest {

    private final ProductionSecretsValidator validator = new ProductionSecretsValidator();

    @BeforeEach
    void setWeakSecret() {
        ReflectionTestUtils.setField(validator, "jwtSecret",
                "flowiq-dev-secret-key-change-in-production-min-256-bits-long!!");
    }

    @Test
    void rejectsKnownWeakJwtSecret() {
        assertThatThrownBy(() -> validator.onApplicationEvent(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Refusing to start with prod profile");
    }

    @Test
    void rejectsBlankJwtSecret() {
        ReflectionTestUtils.setField(validator, "jwtSecret", "   ");
        assertThatThrownBy(() -> validator.onApplicationEvent(null))
                .isInstanceOf(IllegalStateException.class);
    }
}
