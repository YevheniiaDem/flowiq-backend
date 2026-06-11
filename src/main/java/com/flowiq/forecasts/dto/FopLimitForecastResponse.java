package com.flowiq.forecasts.dto;

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
@Schema(description = "FOP income limit usage forecast")
public class FopLimitForecastResponse {

    private int fopGroup;
    private String fopGroupLabel;
    private BigDecimal incomeLimit;
    private BigDecimal currentAnnualIncome;
    private double currentUsagePercent;
    private int monthsUntilLimitExceeded;
    private List<FopLimitHorizonDto> horizons;
}
