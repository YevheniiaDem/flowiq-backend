package com.flowiq.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "FOP-specific tax and compliance insights")
public class FopInsightsResponse {

    private String currentFopGroup;
    private int fopGroupNumber;
    private BigDecimal annualIncome;
    private BigDecimal incomeLimit;
    private double incomeLimitUsagePercent;
    private BigDecimal estimatedTaxLoad;
    private int daysUntilNextTaxPayment;
    private String nextTaxPaymentLabel;
    private double incomeLimitProgress;
    private List<CategoryAmountResponse> topExpenseCategories;
    private BigDecimal taxForecast;
}
