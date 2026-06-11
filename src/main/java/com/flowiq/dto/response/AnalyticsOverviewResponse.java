package com.flowiq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "High-level analytics overview")
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
