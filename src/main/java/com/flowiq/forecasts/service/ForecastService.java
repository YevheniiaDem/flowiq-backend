package com.flowiq.forecasts.service;

import com.flowiq.config.AppPreferences;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.forecasts.dto.*;
import com.flowiq.forecasts.engine.ForecastEngine;
import com.flowiq.forecasts.engine.MonthlyFinancialData;
import com.flowiq.forecasts.engine.TrendAnalysis;
import com.flowiq.forecasts.provider.ForecastProvider;
import com.flowiq.forecasts.provider.RuleBasedForecastProvider;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.UserPrincipal;
import com.flowiq.service.TransactionSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ForecastService {

    private static final int HISTORY_MONTHS = 12;
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
    private final ForecastEngine forecastEngine;
    private final RuleBasedForecastProvider ruleBasedProvider;

    @Autowired(required = false)
    private List<ForecastProvider> forecastProviders;

    public ForecastMetricResponse getRevenueForecast() {
        ForecastData data = loadForecastData();
        return buildMetricResponse(
                data.historical(),
                data.projected(),
                MonthlyFinancialData::getRevenue,
                data.revenueTrend().getGrowthPercent()
        );
    }

    public ForecastMetricResponse getExpenseForecast() {
        ForecastData data = loadForecastData();
        return buildMetricResponse(
                data.historical(),
                data.projected(),
                MonthlyFinancialData::getExpenses,
                data.expenseTrend().getGrowthPercent()
        );
    }

    public ForecastMetricResponse getProfitForecast() {
        ForecastData data = loadForecastData();
        List<MonthlyFinancialData> historicalProfit = data.historical().stream()
                .map(m -> MonthlyFinancialData.builder()
                        .month(m.getMonth())
                        .revenue(m.getRevenue())
                        .expenses(m.getExpenses())
                        .build())
                .toList();
        List<MonthlyFinancialData> projectedProfit = data.projected();

        return buildMetricResponse(
                historicalProfit,
                projectedProfit,
                MonthlyFinancialData::getProfit,
                data.profitTrendPercent()
        );
    }

    public TaxForecastResponse getTaxForecast() {
        ForecastData data = loadForecastData();
        boolean uk = AppPreferences.current().isUkrainian();

        BigDecimal currentTax = estimateTaxLoad(data.ytdRevenue(), data.fopGroup());
        BigDecimal annualTax = forecastAnnualTax(data.ytdRevenue(), data.fopGroup());

        List<BigDecimal> projectedMonthlyTax = data.projected().stream()
                .map(m -> estimateMonthlyTax(m.getRevenue(), data.fopGroup()))
                .toList();

        List<ForecastHorizonDto> horizons = buildHorizons(projectedMonthlyTax, null);
        List<TaxForecastCardDto> cards = new ArrayList<>();

        for (int horizon : ForecastEngine.FORECAST_HORIZONS) {
            BigDecimal projected = forecastEngine.sumHorizon(projectedMonthlyTax, horizon);
            double change = percentChange(projected, currentTax);
            cards.add(TaxForecastCardDto.builder()
                    .months(horizon)
                    .label(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"), "%d міс.", horizon)
                            : String.format(Locale.US, "%d months", horizon))
                    .projectedTax(projected)
                    .changePercent(round(change))
                    .build());
        }

        double taxTrend = percentChange(
                projectedMonthlyTax.isEmpty() ? BigDecimal.ZERO : projectedMonthlyTax.get(0),
                currentTax.divide(new BigDecimal(Math.max(1, LocalDate.now().getMonthValue())), 2, RoundingMode.HALF_UP)
        );

        return TaxForecastResponse.builder()
                .currentTaxBurden(currentTax)
                .annualTaxForecast(annualTax)
                .trendPercent(round(taxTrend))
                .fopGroup(data.fopGroup())
                .horizons(horizons)
                .cards(cards)
                .build();
    }

    public FopLimitForecastResponse getFopLimitForecast() {
        ForecastData data = loadForecastData();
        boolean uk = AppPreferences.current().isUkrainian();

        BigDecimal incomeLimit = data.fopGroup() > 0
                ? INCOME_LIMITS.get(data.fopGroup())
                : INCOME_LIMITS.get(3);

        double usagePercent = incomeLimit.compareTo(BigDecimal.ZERO) == 0
                ? 0
                : data.ytdRevenue().multiply(new BigDecimal("100"))
                .divide(incomeLimit, 2, RoundingMode.HALF_UP)
                .doubleValue();

        BigDecimal avgMonthlyRevenue = forecastEngine.rollingAverage(
                data.historical().stream().map(MonthlyFinancialData::getRevenue).toList(),
                ForecastEngine.ROLLING_WINDOW
        );

        int monthsUntilLimit = forecastEngine.estimateMonthsUntilLimit(
                data.ytdRevenue(), incomeLimit, avgMonthlyRevenue
        );

        List<FopLimitHorizonDto> horizons = new ArrayList<>();
        BigDecimal cumulative = data.ytdRevenue();

        for (int horizon : ForecastEngine.FORECAST_HORIZONS) {
            BigDecimal projectedRevenue = forecastEngine.sumHorizon(
                    data.projected().stream().map(MonthlyFinancialData::getRevenue).toList(),
                    horizon
            );
            BigDecimal projectedAnnual = cumulative.add(projectedRevenue);
            double projectedUsage = incomeLimit.compareTo(BigDecimal.ZERO) == 0
                    ? 0
                    : projectedAnnual.multiply(new BigDecimal("100"))
                    .divide(incomeLimit, 2, RoundingMode.HALF_UP)
                    .doubleValue();

            horizons.add(FopLimitHorizonDto.builder()
                    .months(horizon)
                    .projectedAnnualIncome(projectedAnnual.setScale(2, RoundingMode.HALF_UP))
                    .projectedUsagePercent(projectedUsage)
                    .limitExceeded(projectedUsage >= 100)
                    .build());
        }

        return FopLimitForecastResponse.builder()
                .fopGroup(data.fopGroup())
                .fopGroupLabel(resolveFopGroupLabel(data.fopGroup(), uk))
                .incomeLimit(incomeLimit)
                .currentAnnualIncome(data.ytdRevenue())
                .currentUsagePercent(usagePercent)
                .monthsUntilLimitExceeded(monthsUntilLimit)
                .horizons(horizons)
                .build();
    }

    public ForecastSummaryResponse getSummary() {
        ForecastData data = loadForecastData();
        ForecastProvider.ForecastContext context = buildContext(data);

        List<ForecastInsightDto> insights = new ArrayList<>(ruleBasedProvider.generateInsights(context));
        if (forecastProviders != null) {
            for (ForecastProvider provider : forecastProviders) {
                if (!(provider instanceof RuleBasedForecastProvider)) {
                    insights.addAll(provider.generateInsights(context));
                }
            }
        }

        List<ForecastWarningDto> warnings = ruleBasedProvider.generateWarnings(context);

        List<BigDecimal> projectedRevenue = data.projected().stream()
                .map(MonthlyFinancialData::getRevenue).toList();
        List<BigDecimal> projectedExpenses = data.projected().stream()
                .map(MonthlyFinancialData::getExpenses).toList();
        List<BigDecimal> projectedProfit = data.projected().stream()
                .map(MonthlyFinancialData::getProfit).toList();
        List<BigDecimal> projectedTax = data.projected().stream()
                .map(m -> estimateMonthlyTax(m.getRevenue(), data.fopGroup())).toList();

        return ForecastSummaryResponse.builder()
                .expectedRevenue(forecastEngine.sumHorizon(projectedRevenue, 3))
                .expectedExpenses(forecastEngine.sumHorizon(projectedExpenses, 3))
                .expectedProfit(forecastEngine.sumHorizon(projectedProfit, 3))
                .expectedTax(forecastEngine.sumHorizon(projectedTax, 3))
                .revenueTrendPercent(data.revenueTrend().getGrowthPercent())
                .expenseTrendPercent(data.expenseTrend().getGrowthPercent())
                .profitTrendPercent(data.profitTrendPercent())
                .fopLimitUsagePercent(context.fopLimitUsagePercent())
                .monthsUntilFopLimit(context.monthsUntilFopLimit())
                .revenueHorizons(buildHorizons(projectedRevenue, data.historical().stream()
                        .map(MonthlyFinancialData::getRevenue).toList()))
                .profitHorizons(buildHorizons(projectedProfit, data.historical().stream()
                        .map(MonthlyFinancialData::getProfit).toList()))
                .insights(insights)
                .warnings(warnings)
                .build();
    }

    public ForecastSnapshotResponse getSnapshot() {
        ForecastData data = loadForecastData();
        List<BigDecimal> projectedRevenue = data.projected().stream()
                .map(MonthlyFinancialData::getRevenue).toList();
        List<BigDecimal> projectedProfit = data.projected().stream()
                .map(MonthlyFinancialData::getProfit).toList();
        List<BigDecimal> projectedTax = data.projected().stream()
                .map(m -> estimateMonthlyTax(m.getRevenue(), data.fopGroup())).toList();

        return ForecastSnapshotResponse.builder()
                .expectedRevenue(forecastEngine.sumHorizon(projectedRevenue, 3))
                .expectedProfit(forecastEngine.sumHorizon(projectedProfit, 3))
                .taxForecast(forecastEngine.sumHorizon(projectedTax, 3))
                .revenueTrendPercent(data.revenueTrend().getGrowthPercent())
                .forecastMonths(3)
                .build();
    }

    private ForecastData loadForecastData() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);

        List<MonthlyFinancialData> historical = buildHistoricalData(user.getId());
        List<MonthlyFinancialData> projected = forecastEngine.projectMonths(
                historical, ForecastEngine.PROJECTION_MONTHS);

        List<BigDecimal> revenueValues = historical.stream()
                .map(MonthlyFinancialData::getRevenue).toList();
        List<BigDecimal> expenseValues = historical.stream()
                .map(MonthlyFinancialData::getExpenses).toList();

        TrendAnalysis revenueTrend = forecastEngine.analyzeTrend(revenueValues);
        TrendAnalysis expenseTrend = forecastEngine.analyzeTrend(expenseValues);
        double profitTrend = ruleBasedProvider.calculateProfitTrend(historical);

        LocalDate yearStart = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        BigDecimal ytdRevenue = sumRange(user.getId(), Transaction.Type.REVENUE, yearStart, LocalDate.now());
        int fopGroup = resolveFopGroup(ytdRevenue);

        return new ForecastData(
                historical, projected, revenueTrend, expenseTrend, profitTrend, ytdRevenue, fopGroup
        );
    }

    private List<MonthlyFinancialData> buildHistoricalData(Long userId) {
        List<MonthlyFinancialData> historical = new ArrayList<>();
        YearMonth current = YearMonth.now();

        for (int i = HISTORY_MONTHS - 1; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            BigDecimal revenue = sum(userId, Transaction.Type.REVENUE, month);
            BigDecimal expenses = sum(userId, Transaction.Type.EXPENSE, month);
            historical.add(MonthlyFinancialData.builder()
                    .month(month.toString())
                    .revenue(revenue)
                    .expenses(expenses)
                    .build());
        }

        return historical;
    }

    private ForecastMetricResponse buildMetricResponse(
            List<MonthlyFinancialData> historical,
            List<MonthlyFinancialData> projected,
            Function<MonthlyFinancialData, BigDecimal> extractor,
            double trendPercent
    ) {
        List<ForecastDataPointDto> historicalPoints = historical.stream()
                .map(m -> ForecastDataPointDto.builder()
                        .month(m.getMonth())
                        .amount(extractor.apply(m))
                        .forecast(false)
                        .build())
                .toList();

        List<ForecastDataPointDto> projectedPoints = projected.stream()
                .map(m -> ForecastDataPointDto.builder()
                        .month(m.getMonth())
                        .amount(extractor.apply(m))
                        .forecast(true)
                        .build())
                .toList();

        List<BigDecimal> projectedValues = projected.stream()
                .map(extractor)
                .toList();
        List<BigDecimal> historicalValues = historical.stream()
                .map(extractor)
                .toList();

        return ForecastMetricResponse.builder()
                .historical(historicalPoints)
                .projected(projectedPoints)
                .trendPercent(round(trendPercent))
                .horizons(buildHorizons(projectedValues, historicalValues))
                .build();
    }

    private List<ForecastHorizonDto> buildHorizons(List<BigDecimal> projected, List<BigDecimal> historical) {
        List<ForecastHorizonDto> horizons = new ArrayList<>();
        BigDecimal recentBaseline = historical != null && !historical.isEmpty()
                ? forecastEngine.rollingAverage(historical, ForecastEngine.ROLLING_WINDOW)
                : BigDecimal.ZERO;

        for (int horizon : ForecastEngine.FORECAST_HORIZONS) {
            BigDecimal total = forecastEngine.sumHorizon(projected, horizon);
            Double change = recentBaseline.compareTo(BigDecimal.ZERO) == 0
                    ? null
                    : round(percentChange(
                    total.divide(new BigDecimal(horizon), 2, RoundingMode.HALF_UP),
                    recentBaseline
            ));
            horizons.add(ForecastHorizonDto.builder()
                    .months(horizon)
                    .total(total)
                    .changePercent(change)
                    .build());
        }

        return horizons;
    }

    private ForecastProvider.ForecastContext buildContext(ForecastData data) {
        boolean uk = AppPreferences.current().isUkrainian();
        BigDecimal incomeLimit = data.fopGroup() > 0
                ? INCOME_LIMITS.get(data.fopGroup())
                : INCOME_LIMITS.get(3);

        double usagePercent = incomeLimit.compareTo(BigDecimal.ZERO) == 0
                ? 0
                : data.ytdRevenue().multiply(new BigDecimal("100"))
                .divide(incomeLimit, 2, RoundingMode.HALF_UP)
                .doubleValue();

        BigDecimal avgMonthlyRevenue = forecastEngine.rollingAverage(
                data.historical().stream().map(MonthlyFinancialData::getRevenue).toList(),
                ForecastEngine.ROLLING_WINDOW
        );

        int monthsUntilLimit = forecastEngine.estimateMonthsUntilLimit(
                data.ytdRevenue(), incomeLimit, avgMonthlyRevenue
        );

        BigDecimal currentTax = estimateTaxLoad(data.ytdRevenue(), data.fopGroup());
        BigDecimal projectedTax = data.projected().isEmpty()
                ? BigDecimal.ZERO
                : estimateMonthlyTax(data.projected().get(0).getRevenue(), data.fopGroup());

        double taxTrend = percentChange(projectedTax,
                currentTax.divide(new BigDecimal(Math.max(1, LocalDate.now().getMonthValue())), 2, RoundingMode.HALF_UP));

        return new ForecastProvider.ForecastContext(
                uk,
                data.historical(),
                data.projected(),
                data.revenueTrend().getGrowthPercent(),
                data.expenseTrend().getGrowthPercent(),
                data.profitTrendPercent(),
                data.ytdRevenue(),
                incomeLimit,
                usagePercent,
                monthsUntilLimit,
                currentTax,
                projectedTax,
                taxTrend,
                data.fopGroup()
        );
    }

    private int resolveFopGroup(BigDecimal annualIncome) {
        if (annualIncome.compareTo(INCOME_LIMITS.get(1)) <= 0) return 1;
        if (annualIncome.compareTo(INCOME_LIMITS.get(2)) <= 0) return 2;
        if (annualIncome.compareTo(INCOME_LIMITS.get(3)) <= 0) return 3;
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

    private BigDecimal estimateMonthlyTax(BigDecimal monthlyRevenue, int fopGroup) {
        if (monthlyRevenue.compareTo(BigDecimal.ZERO) <= 0 || fopGroup == 0) {
            return ESV_MONTHLY;
        }
        return monthlyRevenue.multiply(SINGLE_TAX_RATES.get(fopGroup))
                .add(ESV_MONTHLY)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal forecastAnnualTax(BigDecimal ytdIncome, int fopGroup) {
        if (ytdIncome.compareTo(BigDecimal.ZERO) <= 0 || fopGroup == 0) {
            return BigDecimal.ZERO;
        }
        LocalDate today = LocalDate.now();
        int dayOfYear = today.getDayOfYear();
        int daysInYear = today.isLeapYear() ? 366 : 365;
        BigDecimal projectedIncome = ytdIncome
                .multiply(new BigDecimal(daysInYear))
                .divide(new BigDecimal(dayOfYear), 2, RoundingMode.HALF_UP);
        BigDecimal singleTax = projectedIncome.multiply(SINGLE_TAX_RATES.get(fopGroup));
        return singleTax.add(ESV_MONTHLY.multiply(new BigDecimal("12")))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveFopGroupLabel(int fopGroup, boolean uk) {
        if (fopGroup == 0) {
            return uk ? "Загальна система" : "General Tax System";
        }
        return uk ? String.format("ФОП Група %d", fopGroup) : String.format("FOP Group %d", fopGroup);
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

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private record ForecastData(
            List<MonthlyFinancialData> historical,
            List<MonthlyFinancialData> projected,
            TrendAnalysis revenueTrend,
            TrendAnalysis expenseTrend,
            double profitTrendPercent,
            BigDecimal ytdRevenue,
            int fopGroup
    ) {}
}
