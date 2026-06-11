package com.flowiq.forecasts.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Component
public class ForecastEngine {

    public static final int[] FORECAST_HORIZONS = {1, 3, 6, 12};
    public static final int ROLLING_WINDOW = 3;
    public static final int TREND_WINDOW = 6;
    public static final int PROJECTION_MONTHS = 12;

    public TrendAnalysis analyzeTrend(List<BigDecimal> values) {
        if (values == null || values.size() < 2) {
            return TrendAnalysis.builder().growthPercent(0).monthlyGrowthRate(0).build();
        }

        int window = Math.min(TREND_WINDOW, values.size());
        List<BigDecimal> recent = values.subList(values.size() - window, values.size());
        int half = Math.max(1, window / 2);
        BigDecimal recentAvg = rollingAverage(recent, half);
        BigDecimal olderAvg = rollingAverage(recent.subList(0, half), half);

        double growthPercent = percentChange(recentAvg, olderAvg);
        double monthlyGrowthRate = growthPercent / Math.max(1, half);

        return TrendAnalysis.builder()
                .growthPercent(round(growthPercent))
                .monthlyGrowthRate(round(monthlyGrowthRate))
                .build();
    }

    public BigDecimal rollingAverage(List<BigDecimal> values, int window) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        int count = Math.min(window, values.size());
        BigDecimal sum = values.subList(values.size() - count, values.size()).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
    }

    public List<MonthlyFinancialData> projectMonths(List<MonthlyFinancialData> historical, int monthsAhead) {
        List<MonthlyFinancialData> projected = new ArrayList<>();
        if (historical.isEmpty() || monthsAhead <= 0) {
            return projected;
        }

        List<BigDecimal> revenueHistory = historical.stream().map(MonthlyFinancialData::getRevenue).toList();
        List<BigDecimal> expenseHistory = historical.stream().map(MonthlyFinancialData::getExpenses).toList();

        TrendAnalysis revenueTrend = analyzeTrend(revenueHistory);
        TrendAnalysis expenseTrend = analyzeTrend(expenseHistory);

        BigDecimal baseRevenue = rollingAverage(revenueHistory, ROLLING_WINDOW);
        BigDecimal baseExpenses = rollingAverage(expenseHistory, ROLLING_WINDOW);

        YearMonth cursor = YearMonth.parse(historical.get(historical.size() - 1).getMonth()).plusMonths(1);
        BigDecimal revenue = baseRevenue;
        BigDecimal expenses = baseExpenses;

        for (int i = 0; i < monthsAhead; i++) {
            revenue = applyGrowth(revenue, revenueTrend.getMonthlyGrowthRate());
            expenses = applyGrowth(expenses, expenseTrend.getMonthlyGrowthRate());

            projected.add(MonthlyFinancialData.builder()
                    .month(cursor.toString())
                    .revenue(revenue.setScale(2, RoundingMode.HALF_UP))
                    .expenses(expenses.setScale(2, RoundingMode.HALF_UP))
                    .build());
            cursor = cursor.plusMonths(1);
        }

        return projected;
    }

    public BigDecimal sumHorizon(List<BigDecimal> projectedValues, int months) {
        if (projectedValues == null || projectedValues.isEmpty() || months <= 0) {
            return BigDecimal.ZERO;
        }
        int count = Math.min(months, projectedValues.size());
        return projectedValues.subList(0, count).stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public int estimateMonthsUntilLimit(BigDecimal currentIncome, BigDecimal incomeLimit, BigDecimal monthlyRevenue) {
        if (incomeLimit.compareTo(BigDecimal.ZERO) <= 0 || monthlyRevenue.compareTo(BigDecimal.ZERO) <= 0) {
            return -1;
        }
        if (currentIncome.compareTo(incomeLimit) >= 0) {
            return 0;
        }
        BigDecimal remaining = incomeLimit.subtract(currentIncome);
        return remaining.divide(monthlyRevenue, 0, RoundingMode.CEILING).intValue();
    }

    private BigDecimal applyGrowth(BigDecimal value, double monthlyGrowthPercent) {
        BigDecimal factor = BigDecimal.ONE.add(
                BigDecimal.valueOf(monthlyGrowthPercent / 100.0));
        return value.multiply(factor);
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
}
