package com.flowiq.forecasts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Forecast for a single financial metric")
public class ForecastMetricResponse {

    private List<ForecastDataPointDto> historical;
    private List<ForecastDataPointDto> projected;
    private double trendPercent;
    private List<ForecastHorizonDto> horizons;
}
