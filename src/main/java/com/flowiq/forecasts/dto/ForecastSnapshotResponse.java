package com.flowiq.forecasts.dto;

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
@Schema(description = "Dashboard forecast snapshot widget data")
public class ForecastSnapshotResponse {

    private BigDecimal expectedRevenue;
    private BigDecimal expectedProfit;
    private BigDecimal taxForecast;
    private double revenueTrendPercent;
    private int forecastMonths;
}
