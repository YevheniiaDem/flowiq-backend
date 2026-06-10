package com.flowiq.service;

import com.flowiq.analytics.AnalyticsInsightProvider;
import com.flowiq.config.AppPreferences;
import com.flowiq.dto.response.AnalyticsOverviewResponse;
import com.flowiq.dto.response.CategoryAmountResponse;
import com.flowiq.dto.response.FopInsightsResponse;
import com.flowiq.dto.response.MonthlyAmountResponse;
import com.flowiq.dto.response.MonthlyComparisonResponse;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final int TREND_MONTHS = 12;
    private static final BigDecimal ESV_MONTHLY = new BigDecimal("1760");
    private static final Map<Integer, BigDecimal> INCOME_LIMITS = Map.of(
            1, new BigDecimal("1672000"),
            2, new BigDecimal("5328000"),
            3, new BigDecimal("7818000")
    );
    private static final Map<Integer, BigDecimal> SINGLE_TAX_RATES = Map.of(
            1, new BigDecimal("0.10"),
            2, new BigDecimal("0.05"),
            3, new BigDecimal("0.03")
    );

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final TransactionSeedService transactionSeedService;
    private final List<AnalyticsInsightProvider> insightProviders;

    public AnalyticsService(
            TransactionRepository transactionRepository,
            UserRepository userRepository,
            TransactionSeedService transactionSeedService,
            @Autowired(required = false) List<AnalyticsInsightProvider> insightProviders
    ) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.transactionSeedService = transactionSeedService;
        this.insightProviders = insightProviders != null ? insightProviders : List.of();
    }

    public AnalyticsOverviewResponse getOverview() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);
        LocalDate yearStart = LocalDate.of(current.getYear(), 1, 1);
        LocalDate today = LocalDate.now();

        BigDecimal ytdRevenue = sumRange(user.getId(), Transaction.Type.REVENUE, yearStart, today);
        BigDecimal ytdExpenses = sumRange(user.getId(), Transaction.Type.EXPENSE, yearStart, today);
        BigDecimal ytdProfit = ytdRevenue.subtract(ytdExpenses);

        BigDecimal currentMonthRevenue = sum(user.getId(), Transaction.Type.REVENUE, current);
        BigDecimal previousMonthRevenue = sum(user.getId(), Transaction.Type.REVENUE, previous);
        BigDecimal currentMonthExpenses = sum(user.getId(), Transaction.Type.EXPENSE, current);
        BigDecimal previousMonthExpenses = sum(user.getId(), Transaction.Type.EXPENSE, previous);
        BigDecimal currentMonthProfit = currentMonthRevenue.subtract(currentMonthExpenses);
        BigDecimal previousMonthProfit = previousMonthRevenue.subtract(previousMonthExpenses);

        int fopGroup = resolveFopGroup(ytdRevenue);
        BigDecimal currentTaxBurden = estimateTaxLoad(ytdRevenue, fopGroup);
        BigDecimal previousTaxBase = sumRange(
                user.getId(),
                Transaction.Type.REVENUE,
                yearStart,
                previous.atEndOfMonth()
        );
        BigDecimal previousTaxBurden = estimateTaxLoad(previousTaxBase, resolveFopGroup(previousTaxBase));

        return AnalyticsOverviewResponse.builder()
                .revenue(ytdRevenue)
                .expenses(ytdExpenses)
                .profit(ytdProfit)
                .taxBurden(currentTaxBurden)
                .revenueChangePercent(percentChange(currentMonthRevenue, previousMonthRevenue))
                .expensesChangePercent(percentChange(currentMonthExpenses, previousMonthExpenses))
                .profitChangePercent(percentChange(currentMonthProfit, previousMonthProfit))
                .taxBurdenChangePercent(percentChange(currentTaxBurden, previousTaxBurden))
                .build();
    }

    public List<MonthlyAmountResponse> getRevenueTrend() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);
        return buildMonthlySeries(user.getId(), Transaction.Type.REVENUE);
    }

    public List<CategoryAmountResponse> getExpenseBreakdown() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        YearMonth current = YearMonth.now();
        LocalDate start = current.minusMonths(TREND_MONTHS - 1L).atDay(1);
        LocalDate end = current.atEndOfMonth();

        return transactionRepository.sumExpensesByCategory(user.getId(), start, end).stream()
                .map(row -> CategoryAmountResponse.builder()
                        .category(row.getCategory())
                        .amount(row.getAmount())
                        .build())
                .toList();
    }

    public List<MonthlyAmountResponse> getProfitTrend() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        List<MonthlyAmountResponse> trend = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            BigDecimal revenue = sum(user.getId(), Transaction.Type.REVENUE, month);
            BigDecimal expenses = sum(user.getId(), Transaction.Type.EXPENSE, month);
            trend.add(MonthlyAmountResponse.builder()
                    .month(month.toString())
                    .amount(revenue.subtract(expenses))
                    .build());
        }

        return trend;
    }

    public List<MonthlyComparisonResponse> getIncomeVsExpenses() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        List<MonthlyComparisonResponse> comparison = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            comparison.add(MonthlyComparisonResponse.builder()
                    .month(month.toString())
                    .revenue(sum(user.getId(), Transaction.Type.REVENUE, month))
                    .expenses(sum(user.getId(), Transaction.Type.EXPENSE, month))
                    .build());
        }

        return comparison;
    }

    public FopInsightsResponse getFopInsights() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        LocalDate yearStart = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        LocalDate today = LocalDate.now();
        BigDecimal annualIncome = sumRange(user.getId(), Transaction.Type.REVENUE, yearStart, today);

        int fopGroup = resolveFopGroup(annualIncome);
        BigDecimal incomeLimit = fopGroup > 0
                ? INCOME_LIMITS.get(fopGroup)
                : INCOME_LIMITS.get(3);
        double usagePercent = incomeLimit.compareTo(BigDecimal.ZERO) == 0
                ? 0.0
                : annualIncome.multiply(new BigDecimal("100"))
                .divide(incomeLimit, 2, RoundingMode.HALF_UP)
                .doubleValue();

        BigDecimal estimatedTax = estimateTaxLoad(annualIncome, fopGroup);
        BigDecimal taxForecast = forecastAnnualTax(annualIncome, fopGroup, today);

        LocalDate nextDeadline = resolveNextTaxDeadline(today);
        int daysUntil = (int) ChronoUnit.DAYS.between(today, nextDeadline);

        boolean uk = AppPreferences.current().isUkrainian();
        String groupLabel = resolveFopGroupLabel(fopGroup, uk);
        String paymentLabel = uk
                ? String.format(Locale.forLanguageTag("uk-UA"), "До %s", formatDate(nextDeadline, uk))
                : String.format(Locale.US, "Due by %s", formatDate(nextDeadline, uk));

        List<CategoryAmountResponse> topExpenses = transactionRepository
                .sumExpensesByCategory(user.getId(), yearStart, today)
                .stream()
                .limit(5)
                .map(row -> CategoryAmountResponse.builder()
                        .category(row.getCategory())
                        .amount(row.getAmount())
                        .build())
                .toList();

        return FopInsightsResponse.builder()
                .currentFopGroup(groupLabel)
                .fopGroupNumber(fopGroup)
                .annualIncome(annualIncome)
                .incomeLimit(incomeLimit)
                .incomeLimitUsagePercent(usagePercent)
                .incomeLimitProgress(usagePercent)
                .estimatedTaxLoad(estimatedTax)
                .taxForecast(taxForecast)
                .daysUntilNextTaxPayment(daysUntil)
                .nextTaxPaymentLabel(paymentLabel)
                .topExpenseCategories(topExpenses)
                .build();
    }

    private List<MonthlyAmountResponse> buildMonthlySeries(Long userId, Transaction.Type type) {
        List<MonthlyAmountResponse> trend = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = TREND_MONTHS - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            trend.add(MonthlyAmountResponse.builder()
                    .month(month.toString())
                    .amount(sum(userId, type, month))
                    .build());
        }

        return trend;
    }

    private int resolveFopGroup(BigDecimal annualIncome) {
        if (annualIncome.compareTo(INCOME_LIMITS.get(1)) <= 0) {
            return 1;
        }
        if (annualIncome.compareTo(INCOME_LIMITS.get(2)) <= 0) {
            return 2;
        }
        if (annualIncome.compareTo(INCOME_LIMITS.get(3)) <= 0) {
            return 3;
        }
        return 0;
    }

    private BigDecimal estimateTaxLoad(BigDecimal income, int fopGroup) {
        if (income.compareTo(BigDecimal.ZERO) <= 0 || fopGroup == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal singleTax = income.multiply(SINGLE_TAX_RATES.get(fopGroup));
        BigDecimal esv = ESV_MONTHLY.multiply(new BigDecimal(LocalDate.now().getMonthValue()));
        return singleTax.add(esv).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal forecastAnnualTax(BigDecimal ytdIncome, int fopGroup, LocalDate today) {
        if (ytdIncome.compareTo(BigDecimal.ZERO) <= 0 || fopGroup == 0) {
            return BigDecimal.ZERO;
        }
        int dayOfYear = today.getDayOfYear();
        int daysInYear = today.isLeapYear() ? 366 : 365;
        BigDecimal projectedIncome = ytdIncome
                .multiply(new BigDecimal(daysInYear))
                .divide(new BigDecimal(dayOfYear), 2, RoundingMode.HALF_UP);
        BigDecimal singleTax = projectedIncome.multiply(SINGLE_TAX_RATES.get(fopGroup));
        BigDecimal esv = ESV_MONTHLY.multiply(new BigDecimal("12"));
        return singleTax.add(esv).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate resolveNextTaxDeadline(LocalDate today) {
        int year = today.getYear();
        List<LocalDate> deadlines = List.of(
                LocalDate.of(year, 5, 10),
                LocalDate.of(year, 8, 9),
                LocalDate.of(year, 11, 9),
                LocalDate.of(year + 1, 2, 9)
        );

        for (LocalDate deadline : deadlines) {
            if (!deadline.isBefore(today)) {
                return deadline;
            }
        }
        return LocalDate.of(year + 1, 5, 10);
    }

    private String resolveFopGroupLabel(int fopGroup, boolean uk) {
        if (fopGroup == 0) {
            return uk ? "Загальна система" : "General Tax System";
        }
        return uk ? String.format("ФОП Група %d", fopGroup) : String.format("FOP Group %d", fopGroup);
    }

    private String formatDate(LocalDate date, boolean uk) {
        Locale locale = uk ? Locale.forLanguageTag("uk-UA") : Locale.US;
        return date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", locale));
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
