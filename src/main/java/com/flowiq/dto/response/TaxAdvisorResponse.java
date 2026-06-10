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
public class TaxAdvisorResponse {

    private String currentFopGroup;
    private int fopGroupNumber;
    private double incomeLimitUsagePercent;
    private BigDecimal estimatedTaxes;
    private int daysUntilTaxDeadline;
    private String nextTaxPaymentLabel;
    private BigDecimal forecastTaxAmount;
    private BigDecimal annualIncome;
    private BigDecimal incomeLimit;
}
