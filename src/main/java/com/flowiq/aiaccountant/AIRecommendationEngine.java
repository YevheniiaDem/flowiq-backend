package com.flowiq.aiaccountant;

import com.flowiq.dto.response.AIRecommendationResponse;
import com.flowiq.dto.response.CategoryAmountResponse;
import com.flowiq.util.CurrencyFormatter;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class AIRecommendationEngine {

    public List<AIRecommendationResponse> generate(FinancialSnapshot snapshot) {
        List<AIRecommendationResponse> recommendations = new ArrayList<>();
        boolean uk = snapshot.isUkrainian();

        if (snapshot.getIncomeLimitUsagePercent() > 80) {
            recommendations.add(build(
                    "rec-fop-critical",
                    "CRITICAL",
                    uk ? "Ризик перевищення ліміту ФОП" : "FOP income limit risk",
                    uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Ліміт ФОП використано на %.0f%%. За поточним темпом ви можете перевищити ліміт протягом 2 місяців.",
                            snapshot.getIncomeLimitUsagePercent())
                            : String.format(Locale.US,
                            "FOP limit is %.0f%% used. At the current pace you may exceed the limit within 2 months.",
                            snapshot.getIncomeLimitUsagePercent())
            ));
        }

        if (snapshot.getExpenseChangePercent() > 20) {
            recommendations.add(build(
                    "rec-expense-warning",
                    "WARNING",
                    uk ? "Витрати зростають швидко" : "Expenses are rising quickly",
                    uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Витрати зросли на %.1f%% порівняно з минулим місяцем. Перегляньте найбільші категорії.",
                            snapshot.getExpenseChangePercent())
                            : String.format(Locale.US,
                            "Expenses increased by %.1f%% compared to last month. Review your largest categories.",
                            snapshot.getExpenseChangePercent())
            ));
        }

        CategoryAmountResponse infrastructure = findCategory(snapshot, "Infrastructure");
        if (infrastructure != null && snapshot.getPreviousMonthExpenses().compareTo(BigDecimal.ZERO) > 0) {
            double infraShare = infrastructure.getAmount()
                    .divide(snapshot.getCurrentMonthExpenses().max(BigDecimal.ONE), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();
            if (infraShare > 25 && snapshot.getExpenseChangePercent() > 15) {
                recommendations.add(build(
                        "rec-infra-warning",
                        "WARNING",
                        uk ? "Інфраструктурні витрати зростають" : "Infrastructure costs are rising",
                        uk
                                ? String.format(Locale.forLanguageTag("uk-UA"),
                                "Витрати на інфраструктуру становлять %.0f%% від загальних витрат і зростають.",
                                infraShare)
                                : String.format(Locale.US,
                                "Infrastructure costs are %.0f%% of total expenses and trending up.",
                                infraShare)
                ));
            }
        }

        if (snapshot.isProfitGrowingThreeMonths()) {
            recommendations.add(build(
                    "rec-profit-success",
                    "SUCCESS",
                    uk ? "Прибуток зростає 3 місяці поспіль" : "Profit growing for 3 consecutive months",
                    uk
                            ? "Ваш бізнес демонструє стабільне зростання прибутку. Розгляньте реінвестування в масштабування."
                            : "Your business shows steady profit growth. Consider reinvesting in scaling."
            ));
        }

        double currentTaxBurden = calculateTaxBurdenPercent(snapshot);
        if (currentTaxBurden > snapshot.getAverageTaxBurdenPercent() + 5) {
            recommendations.add(build(
                    "rec-tax-opportunity",
                    "OPPORTUNITY",
                    uk ? "Можна оптимізувати податкове навантаження" : "Tax burden optimization opportunity",
                    uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Податкове навантаження (%.1f%%) вище середнього. Перегляньте витрати та групу ФОП.",
                            currentTaxBurden)
                            : String.format(Locale.US,
                            "Tax burden (%.1f%%) is above average. Review deductible expenses and FOP group.",
                            currentTaxBurden)
            ));
        }

        if (snapshot.getRevenueChangePercent() > 10 && snapshot.getExpenseChangePercent() < snapshot.getRevenueChangePercent()) {
            recommendations.add(build(
                    "rec-growth-opportunity",
                    "OPPORTUNITY",
                    uk ? "Сильне зростання доходу" : "Strong revenue growth",
                    uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Дохід зріс на %.1f%% при контрольованих витратах. Час інвестувати в канали зростання.",
                            snapshot.getRevenueChangePercent())
                            : String.format(Locale.US,
                            "Revenue grew %.1f%% with controlled expenses. Good time to invest in growth channels.",
                            snapshot.getRevenueChangePercent())
            ));
        }

        if (recommendations.isEmpty()) {
            recommendations.add(build(
                    "rec-stable",
                    "SUCCESS",
                    uk ? "Фінанси під контролем" : "Finances are under control",
                    uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Дохід %s, прибуток %s. Продовжуйте відстежувати витрати та ліміт ФОП (%.0f%%).",
                            CurrencyFormatter.format(snapshot.getYtdRevenue()),
                            CurrencyFormatter.format(snapshot.getYtdProfit()),
                            snapshot.getIncomeLimitUsagePercent())
                            : String.format(Locale.US,
                            "Revenue %s, profit %s. Keep monitoring expenses and FOP limit (%.0f%%).",
                            CurrencyFormatter.format(snapshot.getYtdRevenue()),
                            CurrencyFormatter.format(snapshot.getYtdProfit()),
                            snapshot.getIncomeLimitUsagePercent())
            ));
        }

        return recommendations;
    }

    private CategoryAmountResponse findCategory(FinancialSnapshot snapshot, String category) {
        return snapshot.getTopExpenseCategories().stream()
                .filter(c -> category.equalsIgnoreCase(c.getCategory()))
                .findFirst()
                .orElse(null);
    }

    private double calculateTaxBurdenPercent(FinancialSnapshot snapshot) {
        if (snapshot.getYtdRevenue().compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        return snapshot.getEstimatedTaxLoad()
                .divide(snapshot.getYtdRevenue(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    private AIRecommendationResponse build(String id, String type, String title, String description) {
        return AIRecommendationResponse.builder()
                .id(id)
                .type(type)
                .title(title)
                .description(description)
                .build();
    }
}
