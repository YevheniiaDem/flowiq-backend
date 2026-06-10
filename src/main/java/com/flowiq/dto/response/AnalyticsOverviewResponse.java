package com.flowiq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsOverviewResponse {

    private BigDecimal revenue;
    private BigDecimal expenses;
    private BigDecimal profit;
    private BigDecimal taxBurden;
    private double revenueChangePercent;
    private double expensesChangePercent;
    private double profitChangePercent;
    private double taxBurdenChangePercent;
}
