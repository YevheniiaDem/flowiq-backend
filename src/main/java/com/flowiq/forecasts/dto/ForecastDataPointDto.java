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
@Schema(description = "A single data point in a forecast time series")
public class ForecastDataPointDto {

    private String month;
    private BigDecimal amount;
    private boolean forecast;
}
