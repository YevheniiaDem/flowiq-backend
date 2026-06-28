package com.flowiq.unit.forecasts;

import com.flowiq.forecasts.dto.ForecastSeverity;
import com.flowiq.forecasts.engine.MonthlyFinancialData;
import com.flowiq.forecasts.provider.ForecastProvider;
import com.flowiq.forecasts.provider.RuleBasedForecastProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RuleBasedForecastProvider unit tests")
class RuleBasedForecastProviderTest {

    private RuleBasedForecastProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RuleBasedForecastProvider();
    }

    @Test
    @DisplayName("generateInsights covers revenue decline and FOP limit warnings")
    void generateInsights_multipleSignals() {
        var context = new ForecastProvider.ForecastContext(
                false,
                List.of(),
                List.of(),
                -8.0,
                15.0,
                6.0,
                new BigDecimal("4000000"),
                new BigDecimal("5328000"),
                90.0,
                4,
                new BigDecimal("10000"),
                new BigDecimal("12000"),
                12.0,
                2
        );

        var insights = provider.generateInsights(context);

        assertThat(insights).extracting("id")
                .contains("ins-revenue-trend", "ins-fop-limit", "ins-fop-usage", "ins-expense-growth", "ins-profit-positive");
    }

    @Test
    @DisplayName("generateInsights returns default when no signals")
    void generateInsights_default() {
        var context = new ForecastProvider.ForecastContext(
                true, List.of(), List.of(), 0, 0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, 0, 0,
                BigDecimal.ZERO, BigDecimal.ZERO, 0, 1
        );

        var insights = provider.generateInsights(context);

        assertThat(insights).hasSize(1);
        assertThat(insights.get(0).getId()).isEqualTo("ins-default");
    }

    @Test
    @DisplayName("generateWarnings includes critical FOP and revenue decline")
    void generateWarnings() {
        var context = new ForecastProvider.ForecastContext(
                false,
                List.of(),
                List.of(),
                -10.0,
                12.0,
                0,
                BigDecimal.ZERO,
                new BigDecimal("1000000"),
                95,
                2,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                15.0,
                2
        );

        var warnings = provider.generateWarnings(context);

        assertThat(warnings).isNotEmpty();
        assertThat(warnings).anyMatch(w -> "fop-limit-risk".equals(w.getType()));
        assertThat(warnings).anyMatch(w -> w.getSeverity() == ForecastSeverity.CRITICAL || w.getSeverity() == ForecastSeverity.WARNING);
    }

    @Test
    @DisplayName("calculateProfitTrend returns zero for insufficient history")
    void calculateProfitTrend_insufficientData() {
        assertThat(provider.calculateProfitTrend(List.of(
                MonthlyFinancialData.builder()
                        .month("2026-01")
                        .revenue(new BigDecimal("100"))
                        .expenses(BigDecimal.ZERO)
                        .build()
        ))).isZero();
    }

    @Test
    @DisplayName("calculateProfitTrend detects positive trend")
    void calculateProfitTrend_positive() {
        List<MonthlyFinancialData> historical = List.of(
                monthProfit("2026-01", "1000"),
                monthProfit("2026-02", "1100"),
                monthProfit("2026-03", "1200"),
                monthProfit("2026-04", "2000"),
                monthProfit("2026-05", "2200"),
                monthProfit("2026-06", "2500")
        );

        assertThat(provider.calculateProfitTrend(historical)).isGreaterThan(0);
    }

    private MonthlyFinancialData monthProfit(String month, String profit) {
        BigDecimal p = new BigDecimal(profit);
        return MonthlyFinancialData.builder()
                .month(month)
                .revenue(p)
                .expenses(BigDecimal.ZERO)
                .build();
    }
}
