package com.flowiq.service;

import com.flowiq.config.AppPreferences;
import com.flowiq.dto.response.AIInsightResponse;
import com.flowiq.dto.response.AISummaryResponse;
import com.flowiq.dto.response.BusinessHealthResponse;
import com.flowiq.dto.response.CategoryAmountResponse;
import com.flowiq.dto.response.MonthlyAmountResponse;
import com.flowiq.dto.response.StatCardResponse;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.UserPrincipal;
import com.flowiq.util.CurrencyFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionSeedService transactionSeedService;

    public List<StatCardResponse> getStats() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);

        BigDecimal currentRevenue = sum(user.getId(), Transaction.Type.REVENUE, current);
        BigDecimal previousRevenue = sum(user.getId(), Transaction.Type.REVENUE, previous);
        BigDecimal currentExpenses = sum(user.getId(), Transaction.Type.EXPENSE, current);
        BigDecimal previousExpenses = sum(user.getId(), Transaction.Type.EXPENSE, previous);

        BigDecimal currentProfit = currentRevenue.subtract(currentExpenses);
        BigDecimal previousProfit = previousRevenue.subtract(previousExpenses);
        BigDecimal currentCashFlow = currentProfit.multiply(new BigDecimal("0.90"));
        BigDecimal previousCashFlow = previousProfit.multiply(new BigDecimal("0.90"));

        return List.of(
                buildStat("revenue", currentRevenue, previousRevenue, "dollar-sign", true),
                buildStat("expenses", currentExpenses, previousExpenses, "trending-down", false),
                buildStat("profit", currentProfit, previousProfit, "activity", true),
                buildStat("cashFlow", currentCashFlow, previousCashFlow, "bar-chart-3", true)
        );
    }

    public List<AIInsightResponse> getInsights() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);

        BigDecimal currentRevenue = sum(user.getId(), Transaction.Type.REVENUE, current);
        BigDecimal previousRevenue = sum(user.getId(), Transaction.Type.REVENUE, previous);
        BigDecimal currentExpenses = sum(user.getId(), Transaction.Type.EXPENSE, current);
        BigDecimal previousExpenses = sum(user.getId(), Transaction.Type.EXPENSE, previous);

        double revenueGrowth = percentChange(currentRevenue, previousRevenue);
        double expenseGrowth = percentChange(currentExpenses, previousExpenses);
        BigDecimal profit = currentRevenue.subtract(currentExpenses);

        boolean uk = AppPreferences.current().isUkrainian();
        List<AIInsightResponse> insights = new ArrayList<>();

        if (expenseGrowth > revenueGrowth && expenseGrowth > 10) {
            insights.add(AIInsightResponse.builder()
                    .id("ins-001")
                    .type("warning")
                    .category("expenses")
                    .title(uk ? "Витрати на маркетинг зростають швидше за дохід"
                            : "Marketing costs rising faster than revenue")
                    .description(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Витрати зросли на %.1f%%, тоді як дохід — на %.1f%%. Перегляньте ROI маркетингу.",
                            expenseGrowth, revenueGrowth)
                            : String.format(Locale.US,
                            "Expenses grew by %.1f%% while revenue grew by %.1f%%. Review marketing ROI.",
                            expenseGrowth, revenueGrowth))
                    .impact("high")
                    .timestamp(uk ? "2 години тому" : "2 hours ago")
                    .icon("alert-triangle")
                    .actionable(true)
                    .build());
        }

        if (revenueGrowth > 0) {
            insights.add(AIInsightResponse.builder()
                    .id("ins-002")
                    .type("success")
                    .category("revenue")
                    .title(uk ? "Дохід зростає" : "Revenue is growing")
                    .description(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Дохід зріс на %.1f%% порівняно з минулим місяцем. Підтримуйте темп на ключових каналах.",
                            revenueGrowth)
                            : String.format(Locale.US,
                            "Revenue increased by %.1f%% compared to last month. Keep momentum on top channels.",
                            revenueGrowth))
                    .impact("high")
                    .timestamp(uk ? "5 годин тому" : "5 hours ago")
                    .icon("check-circle-2")
                    .actionable(false)
                    .build());
        }

        if (profit.compareTo(BigDecimal.ZERO) > 0) {
            insights.add(AIInsightResponse.builder()
                    .id("ins-003")
                    .type("info")
                    .category("cash-flow")
                    .title(uk ? "Позитивний прибуток цього місяця" : "Positive profit this month")
                    .description(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Прибуток за поточний місяць — %s. Розгляньте реінвестування в канали зростання.",
                            CurrencyFormatter.format(profit))
                            : String.format(Locale.US,
                            "Current month profit is %s. Consider reinvesting in growth channels.",
                            CurrencyFormatter.format(profit)))
                    .impact("medium")
                    .timestamp(uk ? "1 день тому" : "1 day ago")
                    .icon("info")
                    .actionable(true)
                    .build());
        }

        if (insights.isEmpty()) {
            insights.add(AIInsightResponse.builder()
                    .id("ins-default")
                    .type("info")
                    .category("operations")
                    .title(uk ? "Початок роботи" : "Getting started")
                    .description(uk
                            ? "Додайте більше транзакцій, щоб отримати глибші AI-інсайти."
                            : "Add more transactions to unlock deeper AI insights.")
                    .impact("low")
                    .timestamp(uk ? "щойно" : "just now")
                    .icon("info")
                    .actionable(false)
                    .build());
        }

        return insights;
    }

    public BusinessHealthResponse getBusinessHealth() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        YearMonth current = YearMonth.now();
        BigDecimal revenue = sum(user.getId(), Transaction.Type.REVENUE, current);
        BigDecimal expenses = sum(user.getId(), Transaction.Type.EXPENSE, current);

        int score = calculateHealthScore(revenue, expenses);

        return BusinessHealthResponse.builder()
                .score(score)
                .maxScore(100)
                .status(resolveHealthStatus(score))
                .build();
    }

    public List<MonthlyAmountResponse> getRevenueTrend() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        List<MonthlyAmountResponse> trend = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            BigDecimal amount = sum(user.getId(), Transaction.Type.REVENUE, month);
            trend.add(MonthlyAmountResponse.builder()
                    .month(month.toString())
                    .amount(amount)
                    .build());
        }

        return trend;
    }

    public List<CategoryAmountResponse> getExpenseBreakdown() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        YearMonth current = YearMonth.now();
        LocalDate start = current.atDay(1);
        LocalDate end = current.atEndOfMonth();

        return transactionRepository.sumExpensesByCategory(user.getId(), start, end).stream()
                .map(row -> CategoryAmountResponse.builder()
                        .category(row.getCategory())
                        .amount(row.getAmount())
                        .build())
                .toList();
    }

    public AISummaryResponse getAISummary() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);

        BigDecimal currentRevenue = sum(user.getId(), Transaction.Type.REVENUE, current);
        BigDecimal previousRevenue = sum(user.getId(), Transaction.Type.REVENUE, previous);
        BigDecimal currentExpenses = sum(user.getId(), Transaction.Type.EXPENSE, current);
        BigDecimal previousExpenses = sum(user.getId(), Transaction.Type.EXPENSE, previous);

        double revenueGrowth = percentChange(currentRevenue, previousRevenue);
        double expenseChange = percentChange(currentExpenses, previousExpenses);

        boolean uk = AppPreferences.current().isUkrainian();
        String text = uk
                ? String.format(Locale.forLanguageTag("uk-UA"),
                "Ваш бізнес демонструє сильну динаміку цього місяця. Дохід змінився на %+.1f%% порівняно з минулим місяцем, витрати — на %+.1f%%. AI виявив можливості для покращення прибутковості.",
                revenueGrowth, expenseChange)
                : String.format(Locale.US,
                "Your business shows strong momentum this month. Revenue changed by %+.1f%% compared to last month, expenses changed by %+.1f%%. AI detected opportunities to improve profitability.",
                revenueGrowth, expenseChange);

        return AISummaryResponse.builder()
                .text(text)
                .badge(uk ? "Активний" : "Active")
                .build();
    }

    private User getCurrentUserEntity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private BigDecimal sum(Long userId, Transaction.Type type, YearMonth month) {
        return transactionRepository.sumByUserAndTypeAndDateRange(
                userId, type, month.atDay(1), month.atEndOfMonth());
    }

    private StatCardResponse buildStat(String labelKey, BigDecimal current, BigDecimal previous,
                                       String icon, boolean higherIsPositive) {
        double change = percentChange(current, previous);
        boolean positive = higherIsPositive ? change >= 0 : change <= 0;

        return StatCardResponse.builder()
                .labelKey(labelKey)
                .amount(current)
                .change(formatPercent(change))
                .changeType(positive ? "positive" : "negative")
                .icon(icon)
                .build();
    }

    private double percentChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    private String formatPercent(double value) {
        boolean uk = AppPreferences.current().isUkrainian();
        Locale locale = uk ? Locale.forLanguageTag("uk-UA") : Locale.US;
        return String.format(locale, "%+.1f%%", value);
    }

    private int calculateHealthScore(BigDecimal revenue, BigDecimal expenses) {
        if (revenue.compareTo(BigDecimal.ZERO) == 0) {
            return 50;
        }
        BigDecimal margin = revenue.subtract(expenses)
                .divide(revenue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        int score = 60 + margin.intValue();
        return Math.min(100, Math.max(0, score));
    }

    private String resolveHealthStatus(int score) {
        if (score >= 90) return "excellent";
        if (score >= 75) return "good";
        if (score >= 60) return "fair";
        return "poor";
    }
}
