package com.flowiq.service;

import com.flowiq.aiaccountant.AIInsightProvider;
import com.flowiq.aiaccountant.AIRecommendationEngine;
import com.flowiq.aiaccountant.FinancialSnapshot;
import com.flowiq.config.AppPreferences;
import com.flowiq.dto.request.AIAccountantChatRequest;
import com.flowiq.dto.response.AIAccountantChatResponse;
import com.flowiq.dto.response.AIAccountantHealthResponse;
import com.flowiq.dto.response.AIRecommendationResponse;
import com.flowiq.dto.response.CategoryAmountResponse;
import com.flowiq.dto.response.ForecastHorizonResponse;
import com.flowiq.dto.response.ForecastsResponse;
import com.flowiq.dto.response.FopInsightsResponse;
import com.flowiq.dto.response.TaxAdvisorResponse;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.UserPrincipal;
import com.flowiq.util.CurrencyFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AIAccountantService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionSeedService transactionSeedService;
    private final AnalyticsService analyticsService;
    private final AIRecommendationEngine recommendationEngine;
    private final List<AIInsightProvider> insightProviders;

    public AIAccountantService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            TransactionSeedService transactionSeedService,
            AnalyticsService analyticsService,
            AIRecommendationEngine recommendationEngine,
            @Autowired(required = false) List<AIInsightProvider> insightProviders
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.transactionSeedService = transactionSeedService;
        this.analyticsService = analyticsService;
        this.recommendationEngine = recommendationEngine;
        this.insightProviders = insightProviders != null ? insightProviders : List.of();
    }

    public AIAccountantHealthResponse getHealth() {
        FinancialSnapshot snapshot = buildSnapshot();
        int score = calculateHealthScore(snapshot);
        String status = resolveHealthStatus(score);
        boolean uk = snapshot.isUkrainian();

        List<String> highlights = new ArrayList<>();
        highlights.add(uk
                ? String.format("Ліміт ФОП використано на %.0f%%", snapshot.getIncomeLimitUsagePercent())
                : String.format("FOP limit used at %.0f%%", snapshot.getIncomeLimitUsagePercent()));
        highlights.add(uk
                ? String.format("Витрати %+.1f%% до минулого місяця", snapshot.getExpenseChangePercent())
                : String.format("Expenses %+.1f%% vs last month", snapshot.getExpenseChangePercent()));
        if (snapshot.getRevenueChangePercent() != 0) {
            highlights.add(uk
                    ? String.format("Дохід %+.1f%% до минулого місяця", snapshot.getRevenueChangePercent())
                    : String.format("Revenue %+.1f%% vs last month", snapshot.getRevenueChangePercent()));
        }

        String summary = uk
                ? (score >= 75
                ? "Ваш бізнес перебуває у хорошому фінансовому стані"
                : score >= 50
                ? "Фінансовий стан задовільний, є зони для покращення"
                : "Потрібна увага до витрат та податкового планування")
                : (score >= 75
                ? "Your business is in good financial health"
                : score >= 50
                ? "Financial health is fair — room for improvement"
                : "Expenses and tax planning need attention");

        return AIAccountantHealthResponse.builder()
                .score(score)
                .status(status)
                .summary(summary)
                .highlights(highlights)
                .build();
    }

    public List<AIRecommendationResponse> getRecommendations() {
        FinancialSnapshot snapshot = buildSnapshot();
        List<AIRecommendationResponse> results = new ArrayList<>(recommendationEngine.generate(snapshot));

        for (AIInsightProvider provider : insightProviders) {
            results.addAll(provider.getRecommendations(snapshot));
        }

        return results;
    }

    public TaxAdvisorResponse getTaxAdvisor() {
        FinancialSnapshot snapshot = buildSnapshot();
        FopInsightsResponse fop = analyticsService.getFopInsights();

        return TaxAdvisorResponse.builder()
                .currentFopGroup(fop.getCurrentFopGroup())
                .fopGroupNumber(fop.getFopGroupNumber())
                .incomeLimitUsagePercent(fop.getIncomeLimitUsagePercent())
                .estimatedTaxes(fop.getEstimatedTaxLoad())
                .daysUntilTaxDeadline(fop.getDaysUntilNextTaxPayment())
                .nextTaxPaymentLabel(fop.getNextTaxPaymentLabel())
                .forecastTaxAmount(fop.getTaxForecast())
                .annualIncome(fop.getAnnualIncome())
                .incomeLimit(fop.getIncomeLimit())
                .build();
    }

    public ForecastsResponse getForecasts() {
        FinancialSnapshot snapshot = buildSnapshot();
        List<ForecastHorizonResponse> horizons = List.of(
                buildForecast(snapshot, 3),
                buildForecast(snapshot, 6),
                buildForecast(snapshot, 12)
        );
        return ForecastsResponse.builder().horizons(horizons).build();
    }

    public AIAccountantChatResponse chat(AIAccountantChatRequest request) {
        FinancialSnapshot snapshot = buildSnapshot();
        String message = request.getMessage().trim();

        for (AIInsightProvider provider : insightProviders) {
            Optional<AIAccountantChatResponse> aiReply = provider.answerChat(snapshot, message);
            if (aiReply.isPresent()) {
                return aiReply.get();
            }
        }

        return AIAccountantChatResponse.builder()
                .reply(generateChatReply(snapshot, message))
                .build();
    }

    private FinancialSnapshot buildSnapshot() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);
        boolean uk = AppPreferences.current().isUkrainian();

        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);
        LocalDate yearStart = LocalDate.of(current.getYear(), 1, 1);
        LocalDate today = LocalDate.now();

        BigDecimal ytdRevenue = sumRange(user.getId(), Transaction.Type.REVENUE, yearStart, today);
        BigDecimal ytdExpenses = sumRange(user.getId(), Transaction.Type.EXPENSE, yearStart, today);
        BigDecimal currentRevenue = sum(user.getId(), Transaction.Type.REVENUE, current);
        BigDecimal currentExpenses = sum(user.getId(), Transaction.Type.EXPENSE, current);
        BigDecimal previousRevenue = sum(user.getId(), Transaction.Type.REVENUE, previous);
        BigDecimal previousExpenses = sum(user.getId(), Transaction.Type.EXPENSE, previous);

        FopInsightsResponse fop = analyticsService.getFopInsights();

        List<BigDecimal> lastThreeProfit = new ArrayList<>();
        for (int i = 2; i >= 0; i--) {
            YearMonth m = current.minusMonths(i);
            lastThreeProfit.add(sum(user.getId(), Transaction.Type.REVENUE, m)
                    .subtract(sum(user.getId(), Transaction.Type.EXPENSE, m)));
        }
        boolean profitGrowingThree = lastThreeProfit.size() == 3
                && lastThreeProfit.get(0).compareTo(lastThreeProfit.get(1)) < 0
                && lastThreeProfit.get(1).compareTo(lastThreeProfit.get(2)) < 0;

        List<FinancialSnapshot.MonthlyTotals> lastTwelve = new ArrayList<>();
        for (int i = 11; i >= 0; i--) {
            YearMonth m = current.minusMonths(i);
            BigDecimal rev = sum(user.getId(), Transaction.Type.REVENUE, m);
            BigDecimal exp = sum(user.getId(), Transaction.Type.EXPENSE, m);
            lastTwelve.add(FinancialSnapshot.MonthlyTotals.builder()
                    .month(m.toString())
                    .revenue(rev)
                    .expenses(exp)
                    .profit(rev.subtract(exp))
                    .build());
        }

        List<CategoryAmountResponse> topExpenses = transactionRepository
                .sumExpensesByCategory(user.getId(), yearStart, today).stream()
                .limit(5)
                .map(row -> CategoryAmountResponse.builder()
                        .category(row.getCategory())
                        .amount(row.getAmount())
                        .build())
                .toList();

        BigDecimal estimatedTax = fop.getEstimatedTaxLoad();
        double avgTaxBurden = ytdRevenue.compareTo(BigDecimal.ZERO) == 0 ? 0
                : estimatedTax.divide(ytdRevenue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).doubleValue() * 0.85;

        return FinancialSnapshot.builder()
                .userId(user.getId())
                .ukrainian(uk)
                .ytdRevenue(ytdRevenue)
                .ytdExpenses(ytdExpenses)
                .ytdProfit(ytdRevenue.subtract(ytdExpenses))
                .currentMonthRevenue(currentRevenue)
                .currentMonthExpenses(currentExpenses)
                .currentMonthProfit(currentRevenue.subtract(currentExpenses))
                .previousMonthRevenue(previousRevenue)
                .previousMonthExpenses(previousExpenses)
                .revenueChangePercent(percentChange(currentRevenue, previousRevenue))
                .expenseChangePercent(percentChange(currentExpenses, previousExpenses))
                .profitChangePercent(percentChange(
                        currentRevenue.subtract(currentExpenses),
                        previousRevenue.subtract(previousExpenses)))
                .fopGroup(fop.getFopGroupNumber())
                .fopGroupLabel(fop.getCurrentFopGroup())
                .annualIncome(fop.getAnnualIncome())
                .incomeLimit(fop.getIncomeLimit())
                .incomeLimitUsagePercent(fop.getIncomeLimitUsagePercent())
                .estimatedTaxLoad(estimatedTax)
                .taxForecast(fop.getTaxForecast())
                .daysUntilTaxDeadline(fop.getDaysUntilNextTaxPayment())
                .profitGrowingThreeMonths(profitGrowingThree)
                .averageTaxBurdenPercent(avgTaxBurden)
                .lastThreeMonthsProfit(lastThreeProfit)
                .topExpenseCategories(topExpenses)
                .lastTwelveMonths(lastTwelve)
                .build();
    }

    private String generateChatReply(FinancialSnapshot s, String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        boolean uk = s.isUkrainian();

        if (matches(lower, "3 місяц", "3 month", "три місяц", "last 3", "останні 3")) {
            BigDecimal threeMonthRevenue = sumLastMonths(s, Transaction.Type.REVENUE, 3);
            return uk
                    ? String.format(Locale.forLanguageTag("uk-UA"),
                    "За останні 3 місяці ваш дохід склав %s, витрати — %s, прибуток — %s.",
                    CurrencyFormatter.format(threeMonthRevenue),
                    CurrencyFormatter.format(sumLastMonths(s, Transaction.Type.EXPENSE, 3)),
                    CurrencyFormatter.format(threeMonthRevenue.subtract(sumLastMonths(s, Transaction.Type.EXPENSE, 3))))
                    : String.format(Locale.US,
                    "Over the last 3 months your revenue was %s, expenses %s, profit %s.",
                    CurrencyFormatter.format(threeMonthRevenue),
                    CurrencyFormatter.format(sumLastMonths(s, Transaction.Type.EXPENSE, 3)),
                    CurrencyFormatter.format(threeMonthRevenue.subtract(sumLastMonths(s, Transaction.Type.EXPENSE, 3))));
        }

        if (matches(lower, "найбільш", "biggest", "largest", "топ", "top", "витрат")) {
            String categories = s.getTopExpenseCategories().stream()
                    .map(c -> c.getCategory() + " (" + CurrencyFormatter.format(c.getAmount()) + ")")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(uk ? "немає даних" : "no data");
            return uk
                    ? "Найбільші категорії витрат: " + categories + "."
                    : "Your largest expense categories: " + categories + ".";
        }

        if (matches(lower, "ліміт", "limit", "фоп", "fop", "досягн")) {
            int monthsToLimit = estimateMonthsToLimit(s);
            return uk
                    ? String.format(Locale.forLanguageTag("uk-UA"),
                    "Ліміт ФОП використано на %.0f%% (%s з %s). За поточним темпом ліміт буде досягнуто приблизно через %d міс.",
                    s.getIncomeLimitUsagePercent(),
                    CurrencyFormatter.format(s.getAnnualIncome()),
                    CurrencyFormatter.format(s.getIncomeLimit()),
                    monthsToLimit)
                    : String.format(Locale.US,
                    "FOP limit is %.0f%% used (%s of %s). At current pace you'll reach the limit in about %d months.",
                    s.getIncomeLimitUsagePercent(),
                    CurrencyFormatter.format(s.getAnnualIncome()),
                    CurrencyFormatter.format(s.getIncomeLimit()),
                    monthsToLimit);
        }

        if (matches(lower, "подат", "tax", "taxes", "єсв", "esv")) {
            return uk
                    ? String.format(Locale.forLanguageTag("uk-UA"),
                    "Орієнтовне податкове навантаження з початку року: %s. Прогноз на рік: %s. Наступний платіж через %d днів (%s).",
                    CurrencyFormatter.format(s.getEstimatedTaxLoad()),
                    CurrencyFormatter.format(s.getTaxForecast()),
                    s.getDaysUntilTaxDeadline(),
                    s.getFopGroupLabel())
                    : String.format(Locale.US,
                    "Estimated tax load YTD: %s. Annual forecast: %s. Next payment in %d days (%s).",
                    CurrencyFormatter.format(s.getEstimatedTaxLoad()),
                    CurrencyFormatter.format(s.getTaxForecast()),
                    s.getDaysUntilTaxDeadline(),
                    s.getFopGroupLabel());
        }

        if (matches(lower, "категор", "categor", "зроста", "grow", "тренд", "trend")) {
            return uk
                    ? String.format(Locale.forLanguageTag("uk-UA"),
                    "Витрати зросли на %.1f%%, дохід — на %.1f%%. Найбільші категорії: %s.",
                    s.getExpenseChangePercent(), s.getRevenueChangePercent(),
                    formatTopCategories(s, uk))
                    : String.format(Locale.US,
                    "Expenses grew %.1f%%, revenue %.1f%%. Top categories: %s.",
                    s.getExpenseChangePercent(), s.getRevenueChangePercent(),
                    formatTopCategories(s, uk));
        }

        if (matches(lower, "дохід", "revenue", "зароб", "earn", "виручк")) {
            return uk
                    ? String.format(Locale.forLanguageTag("uk-UA"),
                    "Дохід з початку року: %s. За поточний місяць: %s (%+.1f%%).",
                    CurrencyFormatter.format(s.getYtdRevenue()),
                    CurrencyFormatter.format(s.getCurrentMonthRevenue()),
                    s.getRevenueChangePercent())
                    : String.format(Locale.US,
                    "YTD revenue: %s. This month: %s (%+.1f%%).",
                    CurrencyFormatter.format(s.getYtdRevenue()),
                    CurrencyFormatter.format(s.getCurrentMonthRevenue()),
                    s.getRevenueChangePercent());
        }

        return uk
                ? String.format(Locale.forLanguageTag("uk-UA"),
                "Я ваш AI-бухгалтер. Дохід YTD: %s, прибуток: %s, ліміт ФОП: %.0f%%. Запитайте про доходи, витрати, податки або прогноз ліміту ФОП.",
                CurrencyFormatter.format(s.getYtdRevenue()),
                CurrencyFormatter.format(s.getYtdProfit()),
                s.getIncomeLimitUsagePercent())
                : String.format(Locale.US,
                "I'm your AI accountant. YTD revenue: %s, profit: %s, FOP limit: %.0f%%. Ask about income, expenses, taxes, or FOP limit forecast.",
                CurrencyFormatter.format(s.getYtdRevenue()),
                CurrencyFormatter.format(s.getYtdProfit()),
                s.getIncomeLimitUsagePercent());
    }

    private String formatTopCategories(FinancialSnapshot s, boolean uk) {
        if (s.getTopExpenseCategories().isEmpty()) {
            return uk ? "немає даних" : "no data";
        }
        return s.getTopExpenseCategories().stream()
                .limit(3)
                .map(c -> c.getCategory())
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private BigDecimal sumLastMonths(FinancialSnapshot s, Transaction.Type type, int months) {
        return s.getLastTwelveMonths().stream()
                .skip(Math.max(0, s.getLastTwelveMonths().size() - months))
                .map(m -> type == Transaction.Type.REVENUE ? m.getRevenue() : m.getExpenses())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int estimateMonthsToLimit(FinancialSnapshot s) {
        if (s.getIncomeLimitUsagePercent() >= 100) {
            return 0;
        }
        BigDecimal remaining = s.getIncomeLimit().subtract(s.getAnnualIncome());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        int monthOfYear = LocalDate.now().getMonthValue();
        if (monthOfYear == 0) {
            return 12;
        }
        BigDecimal monthlyAvg = s.getAnnualIncome().divide(new BigDecimal(monthOfYear), 2, RoundingMode.HALF_UP);
        if (monthlyAvg.compareTo(BigDecimal.ZERO) == 0) {
            return 12;
        }
        return remaining.divide(monthlyAvg, 0, RoundingMode.CEILING).intValue();
    }

    private ForecastHorizonResponse buildForecast(FinancialSnapshot s, int months) {
        BigDecimal avgRevenue = averageLastN(s, months, true);
        BigDecimal avgExpenses = averageLastN(s, months, false);
        double growth = s.getRevenueChangePercent() / 100.0;
        BigDecimal growthFactor = BigDecimal.ONE.add(BigDecimal.valueOf(growth * 0.5));

        BigDecimal revenue = avgRevenue.multiply(growthFactor).multiply(new BigDecimal(months))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal expenses = avgExpenses.multiply(new BigDecimal(months))
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal profit = revenue.subtract(expenses);
        BigDecimal cashFlow = profit.multiply(new BigDecimal("0.90")).setScale(0, RoundingMode.HALF_UP);

        return ForecastHorizonResponse.builder()
                .months(months)
                .revenueForecast(revenue)
                .expenseForecast(expenses)
                .profitForecast(profit)
                .cashFlowForecast(cashFlow)
                .build();
    }

    private BigDecimal averageLastN(FinancialSnapshot s, int n, boolean revenue) {
        List<FinancialSnapshot.MonthlyTotals> months = s.getLastTwelveMonths();
        int count = Math.min(n, months.size());
        if (count == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = months.stream()
                .skip(months.size() - count)
                .map(m -> revenue ? m.getRevenue() : m.getExpenses())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
    }

    private int calculateHealthScore(FinancialSnapshot s) {
        int score = 70;
        if (s.getYtdProfit().compareTo(BigDecimal.ZERO) > 0) {
            score += 10;
        } else {
            score -= 15;
        }
        if (s.getIncomeLimitUsagePercent() > 90) {
            score -= 20;
        } else if (s.getIncomeLimitUsagePercent() > 70) {
            score -= 10;
        }
        if (s.getExpenseChangePercent() > 25) {
            score -= 10;
        }
        if (s.isProfitGrowingThreeMonths()) {
            score += 10;
        }
        if (s.getRevenueChangePercent() > 5) {
            score += 5;
        }
        return Math.min(100, Math.max(0, score));
    }

    private String resolveHealthStatus(int score) {
        if (score >= 85) return "excellent";
        if (score >= 70) return "good";
        if (score >= 50) return "fair";
        return "poor";
    }

    private boolean matches(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
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

    private BigDecimal sumRange(Long userId, Transaction.Type type, LocalDate start, LocalDate end) {
        return transactionRepository.sumByUserAndTypeAndDateRange(userId, type, start, end);
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
}
