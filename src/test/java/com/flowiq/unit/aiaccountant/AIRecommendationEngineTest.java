package com.flowiq.unit.aiaccountant;

import com.flowiq.aiaccountant.AIRecommendationEngine;
import com.flowiq.aiaccountant.FinancialSnapshot;
import com.flowiq.dto.response.AIRecommendationResponse;
import com.flowiq.dto.response.CategoryAmountResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AIRecommendationEngine unit tests")
class AIRecommendationEngineTest {

    private AIRecommendationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new AIRecommendationEngine();
    }

    @Test
    @DisplayName("returns stable recommendation when no risk signals")
    void generate_stableFinances_returnsDefaultSuccess() {
        List<AIRecommendationResponse> recommendations = engine.generate(baseSnapshot().build());

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).getId()).isEqualTo("rec-stable");
        assertThat(recommendations.get(0).getType()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("critical FOP limit warning when usage above 80%")
    void generate_highFopUsage_criticalRecommendation() {
        FinancialSnapshot snapshot = baseSnapshot()
                .incomeLimitUsagePercent(85.0)
                .build();

        List<AIRecommendationResponse> recommendations = engine.generate(snapshot);

        assertThat(recommendations)
                .anyMatch(r -> "rec-fop-critical".equals(r.getId()) && "CRITICAL".equals(r.getType()));
    }

    @Test
    @DisplayName("expense spike warning when expenses grow more than 20%")
    void generate_expenseSpike_warningRecommendation() {
        FinancialSnapshot snapshot = baseSnapshot()
                .expenseChangePercent(25.0)
                .build();

        List<AIRecommendationResponse> recommendations = engine.generate(snapshot);

        assertThat(recommendations)
                .anyMatch(r -> "rec-expense-warning".equals(r.getId()));
    }

    @Test
    @DisplayName("infrastructure cost warning when share exceeds threshold")
    void generate_infrastructureShare_warning() {
        FinancialSnapshot snapshot = baseSnapshot()
                .expenseChangePercent(20.0)
                .currentMonthExpenses(new BigDecimal("10000"))
                .previousMonthExpenses(new BigDecimal("8000"))
                .topExpenseCategories(List.of(
                        CategoryAmountResponse.builder()
                                .category("Infrastructure")
                                .amount(new BigDecimal("3000"))
                                .build()
                ))
                .build();

        List<AIRecommendationResponse> recommendations = engine.generate(snapshot);

        assertThat(recommendations)
                .anyMatch(r -> "rec-infra-warning".equals(r.getId()));
    }

    @Test
    @DisplayName("profit growth success recommendation")
    void generate_profitGrowingThreeMonths_success() {
        FinancialSnapshot snapshot = baseSnapshot()
                .profitGrowingThreeMonths(true)
                .build();

        List<AIRecommendationResponse> recommendations = engine.generate(snapshot);

        assertThat(recommendations)
                .anyMatch(r -> "rec-profit-success".equals(r.getId()));
    }

    @Test
    @DisplayName("tax optimization opportunity when burden above average")
    void generate_highTaxBurden_opportunity() {
        FinancialSnapshot snapshot = baseSnapshot()
                .ytdRevenue(new BigDecimal("500000"))
                .estimatedTaxLoad(new BigDecimal("75000"))
                .averageTaxBurdenPercent(9.0)
                .build();

        List<AIRecommendationResponse> recommendations = engine.generate(snapshot);

        assertThat(recommendations)
                .anyMatch(r -> "rec-tax-opportunity".equals(r.getId()));
    }

    @Test
    @DisplayName("revenue growth opportunity when revenue grows faster than expenses")
    void generate_strongRevenueGrowth_opportunity() {
        FinancialSnapshot snapshot = baseSnapshot()
                .revenueChangePercent(15.0)
                .expenseChangePercent(5.0)
                .build();

        List<AIRecommendationResponse> recommendations = engine.generate(snapshot);

        assertThat(recommendations)
                .anyMatch(r -> "rec-growth-opportunity".equals(r.getId()));
    }

    @Test
    @DisplayName("zero revenue yields zero tax burden percent without error")
    void generate_zeroRevenue_noTaxBurdenError() {
        FinancialSnapshot snapshot = baseSnapshot()
                .ytdRevenue(BigDecimal.ZERO)
                .estimatedTaxLoad(new BigDecimal("1000"))
                .averageTaxBurdenPercent(5.0)
                .build();

        assertThat(engine.generate(snapshot)).isNotEmpty();
    }

    @Test
    @DisplayName("Ukrainian locale uses Ukrainian titles")
    void generate_ukrainianLocale_ukrainianText() {
        FinancialSnapshot snapshot = baseSnapshot()
                .ukrainian(true)
                .incomeLimitUsagePercent(90.0)
                .build();

        AIRecommendationResponse fop = engine.generate(snapshot).stream()
                .filter(r -> "rec-fop-critical".equals(r.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(fop.getTitle()).contains("ФОП");
    }

    @Test
    @DisplayName("English locale uses English titles")
    void generate_englishLocale_englishText() {
        FinancialSnapshot snapshot = baseSnapshot()
                .ukrainian(false)
                .incomeLimitUsagePercent(90.0)
                .build();

        AIRecommendationResponse fop = engine.generate(snapshot).stream()
                .filter(r -> "rec-fop-critical".equals(r.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(fop.getTitle()).contains("FOP");
    }

    @Test
    @DisplayName("multiple recommendations can be returned simultaneously")
    void generate_multipleSignals_multipleRecommendations() {
        FinancialSnapshot snapshot = baseSnapshot()
                .incomeLimitUsagePercent(90.0)
                .expenseChangePercent(30.0)
                .profitGrowingThreeMonths(true)
                .build();

        List<AIRecommendationResponse> recommendations = engine.generate(snapshot);

        assertThat(recommendations.size()).isGreaterThanOrEqualTo(3);
    }

    private FinancialSnapshot.FinancialSnapshotBuilder baseSnapshot() {
        return FinancialSnapshot.builder()
                .userId(1L)
                .ukrainian(true)
                .ytdRevenue(new BigDecimal("300000"))
                .ytdExpenses(new BigDecimal("120000"))
                .ytdProfit(new BigDecimal("180000"))
                .currentMonthRevenue(new BigDecimal("30000"))
                .currentMonthExpenses(new BigDecimal("12000"))
                .currentMonthProfit(new BigDecimal("18000"))
                .previousMonthRevenue(new BigDecimal("28000"))
                .previousMonthExpenses(new BigDecimal("11000"))
                .revenueChangePercent(5.0)
                .expenseChangePercent(5.0)
                .profitChangePercent(5.0)
                .fopGroup(2)
                .incomeLimitUsagePercent(50.0)
                .estimatedTaxLoad(new BigDecimal("15000"))
                .averageTaxBurdenPercent(12.0)
                .profitGrowingThreeMonths(false)
                .topExpenseCategories(List.of());
    }
}
