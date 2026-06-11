package com.flowiq.forecasts.provider;

import com.flowiq.forecasts.dto.ForecastInsightDto;
import com.flowiq.forecasts.dto.ForecastSeverity;
import com.flowiq.forecasts.dto.ForecastWarningDto;
import com.flowiq.forecasts.engine.MonthlyFinancialData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class RuleBasedForecastProvider implements ForecastProvider {

    @Override
    public List<ForecastInsightDto> generateInsights(ForecastContext context) {
        List<ForecastInsightDto> insights = new ArrayList<>();
        boolean uk = context.ukrainian();

        if (Math.abs(context.revenueTrendPercent()) >= 1) {
            String direction = context.revenueTrendPercent() >= 0
                    ? (uk ? "зростання" : "growth")
                    : (uk ? "зниження" : "decline");
            insights.add(ForecastInsightDto.builder()
                    .id("ins-revenue-trend")
                    .category("revenue")
                    .severity(context.revenueTrendPercent() < -5
                            ? ForecastSeverity.WARNING
                            : ForecastSeverity.INFO)
                    .message(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Поточний тренд вказує на %s доходу на %.1f%%.",
                            direction, Math.abs(context.revenueTrendPercent()))
                            : String.format(Locale.US,
                            "Current trend indicates revenue %s of %.1f%%.",
                            direction, Math.abs(context.revenueTrendPercent())))
                    .build());
        }

        if (context.monthsUntilFopLimit() > 0 && context.monthsUntilFopLimit() <= 12) {
            ForecastSeverity severity = context.monthsUntilFopLimit() <= 3
                    ? ForecastSeverity.CRITICAL
                    : context.monthsUntilFopLimit() <= 6
                    ? ForecastSeverity.WARNING
                    : ForecastSeverity.INFO;
            insights.add(ForecastInsightDto.builder()
                    .id("ins-fop-limit")
                    .category("fop-limit")
                    .severity(severity)
                    .message(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "За поточним темпом ліміт ФОП може бути перевищено через %d міс.",
                            context.monthsUntilFopLimit())
                            : String.format(Locale.US,
                            "At current growth rate FOP limit may be exceeded in %d months.",
                            context.monthsUntilFopLimit()))
                    .build());
        }

        if (context.fopLimitUsagePercent() >= 85) {
            insights.add(ForecastInsightDto.builder()
                    .id("ins-fop-usage")
                    .category("fop-limit")
                    .severity(context.fopLimitUsagePercent() >= 95
                            ? ForecastSeverity.CRITICAL
                            : ForecastSeverity.WARNING)
                    .message(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Ліміт ФОП використано на %.0f%%. Плануйте податкову стратегію заздалегідь.",
                            context.fopLimitUsagePercent())
                            : String.format(Locale.US,
                            "FOP limit is %.0f%% used. Plan your tax strategy ahead.",
                            context.fopLimitUsagePercent()))
                    .build());
        }

        if (Math.abs(context.taxTrendPercent()) >= 1) {
            insights.add(ForecastInsightDto.builder()
                    .id("ins-tax-trend")
                    .category("taxes")
                    .severity(context.taxTrendPercent() > 15
                            ? ForecastSeverity.WARNING
                            : ForecastSeverity.INFO)
                    .message(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Податкове навантаження очікується %s на %.1f%%.",
                            context.taxTrendPercent() >= 0 ? "зросте" : "зменшиться",
                            Math.abs(context.taxTrendPercent()))
                            : String.format(Locale.US,
                            "Tax burden expected to %s by %.1f%%.",
                            context.taxTrendPercent() >= 0 ? "increase" : "decrease",
                            Math.abs(context.taxTrendPercent())))
                    .build());
        }

        if (context.expenseTrendPercent() > context.revenueTrendPercent() + 5) {
            insights.add(ForecastInsightDto.builder()
                    .id("ins-expense-growth")
                    .category("expenses")
                    .severity(ForecastSeverity.WARNING)
                    .message(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Витрати зростають швидше за дохід (%.1f%% проти %.1f%%).",
                            context.expenseTrendPercent(), context.revenueTrendPercent())
                            : String.format(Locale.US,
                            "Expenses are growing faster than revenue (%.1f%% vs %.1f%%).",
                            context.expenseTrendPercent(), context.revenueTrendPercent()))
                    .build());
        }

        if (context.profitTrendPercent() > 5) {
            insights.add(ForecastInsightDto.builder()
                    .id("ins-profit-positive")
                    .category("profit")
                    .severity(ForecastSeverity.INFO)
                    .message(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Прогноз прибутку позитивний — тренд зростання %.1f%%.",
                            context.profitTrendPercent())
                            : String.format(Locale.US,
                            "Profit forecast is positive with a %.1f%% growth trend.",
                            context.profitTrendPercent()))
                    .build());
        }

        if (insights.isEmpty()) {
            insights.add(ForecastInsightDto.builder()
                    .id("ins-default")
                    .category("general")
                    .severity(ForecastSeverity.INFO)
                    .message(uk
                            ? "Додайте більше транзакцій для точніших прогнозів."
                            : "Add more transactions for more accurate forecasts.")
                    .build());
        }

        return insights;
    }

    public List<ForecastWarningDto> generateWarnings(ForecastContext context) {
        List<ForecastWarningDto> warnings = new ArrayList<>();
        boolean uk = context.ukrainian();

        if (context.monthsUntilFopLimit() > 0 && context.monthsUntilFopLimit() <= 6) {
            warnings.add(ForecastWarningDto.builder()
                    .type("fop-limit-risk")
                    .severity(context.monthsUntilFopLimit() <= 3
                            ? ForecastSeverity.CRITICAL
                            : ForecastSeverity.WARNING)
                    .title(uk ? "Ризик перевищення ліміту ФОП" : "FOP Limit Risk")
                    .message(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Ліміт може бути досягнуто через %d міс. при поточному темпі доходу.",
                            context.monthsUntilFopLimit())
                            : String.format(Locale.US,
                            "Limit may be reached in %d months at current revenue pace.",
                            context.monthsUntilFopLimit()))
                    .build());
        }

        if (context.revenueTrendPercent() < -5) {
            warnings.add(ForecastWarningDto.builder()
                    .type("revenue-decline")
                    .severity(ForecastSeverity.WARNING)
                    .title(uk ? "Зниження доходу" : "Revenue Decline")
                    .message(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Прогнозується зниження доходу на %.1f%%.",
                            Math.abs(context.revenueTrendPercent()))
                            : String.format(Locale.US,
                            "Revenue decline of %.1f%% is projected.",
                            Math.abs(context.revenueTrendPercent())))
                    .build());
        }

        if (context.expenseTrendPercent() > 10) {
            warnings.add(ForecastWarningDto.builder()
                    .type("expense-growth")
                    .severity(ForecastSeverity.WARNING)
                    .title(uk ? "Зростання витрат" : "Expense Growth")
                    .message(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Витрати зростають на %.1f%% — перевірте бюджет.",
                            context.expenseTrendPercent())
                            : String.format(Locale.US,
                            "Expenses growing at %.1f%% — review your budget.",
                            context.expenseTrendPercent()))
                    .build());
        }

        if (context.taxTrendPercent() > 10) {
            warnings.add(ForecastWarningDto.builder()
                    .type("tax-increase")
                    .severity(ForecastSeverity.WARNING)
                    .title(uk ? "Зростання податкового навантаження" : "Tax Increase")
                    .message(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Податкове навантаження може зрости на %.1f%%.",
                            context.taxTrendPercent())
                            : String.format(Locale.US,
                            "Tax burden may increase by %.1f%%.",
                            context.taxTrendPercent()))
                    .build());
        }

        return warnings;
    }

    public double calculateProfitTrend(List<MonthlyFinancialData> historical) {
        if (historical.size() < 2) {
            return 0;
        }
        List<BigDecimal> profits = historical.stream()
                .map(MonthlyFinancialData::getProfit)
                .toList();
        int window = Math.min(6, profits.size());
        List<BigDecimal> recent = profits.subList(profits.size() - window, profits.size());
        int half = Math.max(1, window / 2);
        BigDecimal recentAvg = average(recent.subList(recent.size() - half, recent.size()));
        BigDecimal olderAvg = average(recent.subList(0, half));
        return percentChange(recentAvg, olderAvg);
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(values.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private double percentChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }
}
