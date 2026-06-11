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
@Schema(description = "Forecast for a specific time horizon")
public class ForecastHorizonResponse {

    private int months;
    private BigDecimal revenueForecast;
    private BigDecimal expenseForecast;
    private BigDecimal profitForecast;
    private BigDecimal cashFlowForecast;
}
