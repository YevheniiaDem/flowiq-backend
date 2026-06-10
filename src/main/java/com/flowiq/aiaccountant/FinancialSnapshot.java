package com.flowiq.aiaccountant;

import com.flowiq.dto.response.CategoryAmountResponse;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class FinancialSnapshot {

    Long userId;
    boolean ukrainian;

    BigDecimal ytdRevenue;
    BigDecimal ytdExpenses;
    BigDecimal ytdProfit;
    BigDecimal currentMonthRevenue;
    BigDecimal currentMonthExpenses;
    BigDecimal currentMonthProfit;
    BigDecimal previousMonthRevenue;
    BigDecimal previousMonthExpenses;

    double revenueChangePercent;
    double expenseChangePercent;
    double profitChangePercent;

    int fopGroup;
    String fopGroupLabel;
    BigDecimal annualIncome;
    BigDecimal incomeLimit;
    double incomeLimitUsagePercent;
    BigDecimal estimatedTaxLoad;
    BigDecimal taxForecast;
    int daysUntilTaxDeadline;

    boolean profitGrowingThreeMonths;
    double averageTaxBurdenPercent;

    List<BigDecimal> lastThreeMonthsProfit;
    List<CategoryAmountResponse> topExpenseCategories;
    List<MonthlyTotals> lastTwelveMonths;

    @Value
    @Builder
    public static class MonthlyTotals {
        String month;
        BigDecimal revenue;
        BigDecimal expenses;
        BigDecimal profit;
    }
}
