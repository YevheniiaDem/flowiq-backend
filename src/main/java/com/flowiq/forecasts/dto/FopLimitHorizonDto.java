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
@Schema(description = "Projected FOP limit usage at a forecast horizon")
public class FopLimitHorizonDto {

    private int months;
    private BigDecimal projectedAnnualIncome;
    private double projectedUsagePercent;
    private boolean limitExceeded;
}
