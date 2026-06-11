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
@Schema(description = "Tax forecast card for a specific horizon")
public class TaxForecastCardDto {

    private int months;
    private String label;
    private BigDecimal projectedTax;
    private double changePercent;
}
