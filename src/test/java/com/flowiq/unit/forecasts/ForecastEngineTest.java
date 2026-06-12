package com.flowiq.unit.forecasts;

import com.flowiq.forecasts.engine.ForecastEngine;
import com.flowiq.forecasts.engine.MonthlyFinancialData;
import com.flowiq.forecasts.engine.TrendAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ForecastEngine unit tests")
class ForecastEngineTest {

    private ForecastEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ForecastEngine();
    }

    @Test
    @DisplayName("analyzeTrend returns zero growth for null input")
    void analyzeTrend_null_returnsZero() {
        TrendAnalysis trend = engine.analyzeTrend(null);

        assertThat(trend.getGrowthPercent()).isZero();
        assertThat(trend.getMonthlyGrowthRate()).isZero();
    }

    @Test
    @DisplayName("analyzeTrend returns zero growth for single value")
    void analyzeTrend_singleValue_returnsZero() {
        TrendAnalysis trend = engine.analyzeTrend(List.of(new BigDecimal("100")));

        assertThat(trend.getGrowthPercent()).isZero();
    }

    @Test
    @DisplayName("analyzeTrend detects positive growth")
    void analyzeTrend_increasingValues_positiveGrowth() {
        List<BigDecimal> values = List.of(
                new BigDecimal("100"), new BigDecimal("110"),
                new BigDecimal("120"), new BigDecimal("130"),
                new BigDecimal("140"), new BigDecimal("150")
        );

        TrendAnalysis trend = engine.analyzeTrend(values);

        assertThat(trend.getGrowthPercent()).isGreaterThan(0);
    }

    @Test
    @DisplayName("analyzeTrend detects negative growth")
    void analyzeTrend_decreasingValues_negativeGrowth() {
        List<BigDecimal> values = List.of(
                new BigDecimal("200"), new BigDecimal("180"),
                new BigDecimal("160"), new BigDecimal("140"),
                new BigDecimal("120"), new BigDecimal("100")
        );

        TrendAnalysis trend = engine.analyzeTrend(values);

        assertThat(trend.getGrowthPercent()).isLessThan(0);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("rollingAverage returns zero for null or empty")
    void rollingAverage_empty_returnsZero(List<BigDecimal> values) {
        assertThat(engine.rollingAverage(values, 3)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("rollingAverage computes average over window")
    void rollingAverage_validWindow() {
        List<BigDecimal> values = List.of(
                new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30")
        );

        assertThat(engine.rollingAverage(values, 3)).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("rollingAverage uses available values when window exceeds size")
    void rollingAverage_windowLargerThanList() {
        List<BigDecimal> values = List.of(new BigDecimal("100"), new BigDecimal("200"));

        assertThat(engine.rollingAverage(values, 10)).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("projectMonths returns empty for empty historical data")
    void projectMonths_emptyHistorical() {
        assertThat(engine.projectMonths(List.of(), 6)).isEmpty();
    }

    @Test
    @DisplayName("projectMonths returns empty for zero months ahead")
    void projectMonths_zeroMonthsAhead() {
        List<MonthlyFinancialData> historical = sampleHistorical(3);

        assertThat(engine.projectMonths(historical, 0)).isEmpty();
    }

    @Test
    @DisplayName("projectMonths generates requested number of months")
    void projectMonths_generatesProjections() {
        List<MonthlyFinancialData> historical = sampleHistorical(6);

        List<MonthlyFinancialData> projected = engine.projectMonths(historical, 3);

        assertThat(projected).hasSize(3);
        assertThat(projected.get(0).getRevenue()).isGreaterThan(BigDecimal.ZERO);
        assertThat(projected.get(0).getExpenses()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("projectMonths continues from month after last historical")
    void projectMonths_startsAfterLastHistoricalMonth() {
        YearMonth last = YearMonth.now().minusMonths(1);
        List<MonthlyFinancialData> historical = List.of(
                MonthlyFinancialData.builder()
                        .month(last.toString())
                        .revenue(new BigDecimal("1000"))
                        .expenses(new BigDecimal("500"))
                        .build()
        );

        List<MonthlyFinancialData> projected = engine.projectMonths(historical, 1);

        assertThat(projected.get(0).getMonth()).isEqualTo(last.plusMonths(1).toString());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    @DisplayName("sumHorizon returns zero for non-positive months")
    void sumHorizon_invalidMonths(int months) {
        List<BigDecimal> values = List.of(new BigDecimal("100"), new BigDecimal("200"));

        assertThat(engine.sumHorizon(values, months)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("sumHorizon sums first N projected values")
    void sumHorizon_sumsCorrectly() {
        List<BigDecimal> values = List.of(
                new BigDecimal("100"), new BigDecimal("200"), new BigDecimal("300")
        );

        assertThat(engine.sumHorizon(values, 2)).isEqualByComparingTo("300.00");
    }

    @Test
    @DisplayName("sumHorizon caps at list size")
    void sumHorizon_capsAtListSize() {
        List<BigDecimal> values = List.of(new BigDecimal("50"));

        assertThat(engine.sumHorizon(values, 12)).isEqualByComparingTo("50.00");
    }

    @ParameterizedTest
    @CsvSource({
            "500000, 0, 50000, -1",
            "500000, 1000000, 0, -1"
    })
    @DisplayName("estimateMonthsUntilLimit returns -1 for invalid limits")
    void estimateMonthsUntilLimit_invalidInputs(
            String current, String limit, String monthly, int expected
    ) {
        int result = engine.estimateMonthsUntilLimit(
                new BigDecimal(current), new BigDecimal(limit), new BigDecimal(monthly));

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("estimateMonthsUntilLimit returns 0 when already at limit")
    void estimateMonthsUntilLimit_atLimit() {
        BigDecimal limit = new BigDecimal("1000000");

        assertThat(engine.estimateMonthsUntilLimit(limit, limit, new BigDecimal("10000"))).isZero();
    }

    @Test
    @DisplayName("estimateMonthsUntilLimit calculates remaining months")
    void estimateMonthsUntilLimit_calculatesMonths() {
        int months = engine.estimateMonthsUntilLimit(
                new BigDecimal("500000"),
                new BigDecimal("1000000"),
                new BigDecimal("100000")
        );

        assertThat(months).isEqualTo(5);
    }

    private List<MonthlyFinancialData> sampleHistorical(int months) {
        List<MonthlyFinancialData> data = new ArrayList<>();
        YearMonth cursor = YearMonth.now().minusMonths(months);
        for (int i = 0; i < months; i++) {
            data.add(MonthlyFinancialData.builder()
                    .month(cursor.plusMonths(i).toString())
                    .revenue(new BigDecimal("10000").add(new BigDecimal(i * 100)))
                    .expenses(new BigDecimal("5000").add(new BigDecimal(i * 50)))
                    .build());
        }
        return data;
    }
}
