package com.flowiq.reports;

import com.flowiq.entity.ReportJob;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class ReportData {

    ReportJob.ReportType reportType;
    String title;
    LocalDate periodFrom;
    LocalDate periodTo;
    BigDecimal revenue;
    BigDecimal expenses;
    BigDecimal profit;
    BigDecimal taxBurden;
    List<CategoryLine> revenueCategories;
    List<CategoryLine> expenseCategories;
    List<MonthlyLine> monthlyLines;
    String fopGroup;
    double incomeLimitUsagePercent;
    BigDecimal estimatedTax;
    BigDecimal taxForecast;
    BigDecimal annualIncome;
    BigDecimal incomeLimit;

    @Value
    @Builder
    public static class CategoryLine {
        String category;
        BigDecimal amount;
    }

    @Value
    @Builder
    public static class MonthlyLine {
        String month;
        BigDecimal revenue;
        BigDecimal expenses;
        BigDecimal profit;
    }
}
