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
@Schema(description = "Tax burden forecast based on projected revenue")
public class TaxForecastResponse {

    private BigDecimal currentTaxBurden;
    private BigDecimal annualTaxForecast;
    private double trendPercent;
    private int fopGroup;
    private List<ForecastHorizonDto> horizons;
    private List<TaxForecastCardDto> cards;
}
