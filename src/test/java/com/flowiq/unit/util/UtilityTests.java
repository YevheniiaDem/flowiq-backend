package com.flowiq.unit.util;

import com.flowiq.config.AppPreferences;
import com.flowiq.exception.BadRequestException;
import com.flowiq.util.CurrencyFormatter;
import com.flowiq.util.TransactionDateValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Utility class tests")
class UtilityTests {

    @AfterEach
    void tearDown() {
        AppPreferences.clear();
    }

    @Test
    @DisplayName("TransactionDateValidator accepts valid date")
    void transactionDateValidator_acceptsValidDate() {
        assertThatCode(() -> TransactionDateValidator.validate(LocalDate.of(2026, 1, 15)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TransactionDateValidator rejects null date")
    void transactionDateValidator_rejectsNull() {
        assertThatThrownBy(() -> TransactionDateValidator.validate(null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Transaction date is required");
    }

    @Test
    @DisplayName("TransactionDateValidator rejects date before 2000")
    void transactionDateValidator_rejectsTooOld() {
        assertThatThrownBy(() -> TransactionDateValidator.validate(LocalDate.of(1999, 12, 31)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must not be before");
    }

    @Test
    @DisplayName("TransactionDateValidator rejects future date")
    void transactionDateValidator_rejectsFuture() {
        assertThatThrownBy(() -> TransactionDateValidator.validate(LocalDate.now().plusDays(1)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Transaction date cannot be in the future");
    }

    @Test
    @DisplayName("CurrencyFormatter formats UAH by default")
    void currencyFormatter_formatsUah() {
        AppPreferences.current().setCurrency("UAH");

        String formatted = CurrencyFormatter.format(new BigDecimal("12345"));

        assertThat(formatted).contains("12");
        assertThat(formatted).contains("\u20B4");
    }

    @Test
    @DisplayName("CurrencyFormatter formats USD when preference set")
    void currencyFormatter_formatsUsd() {
        AppPreferences.current().setCurrency("USD");

        String formatted = CurrencyFormatter.format(new BigDecimal("10000"));

        assertThat(formatted).startsWith("$");
    }

    @Test
    @DisplayName("CurrencyFormatter formats EUR when preference set")
    void currencyFormatter_formatsEur() {
        AppPreferences.current().setCurrency("EUR");

        String formatted = CurrencyFormatter.format(new BigDecimal("10000"));

        assertThat(formatted).isNotBlank();
    }
}
