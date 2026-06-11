package com.flowiq.forecasts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Smart warning banner for forecast risks")
public class ForecastWarningDto {

    private String type;
    private String title;
    private String message;
    private ForecastSeverity severity;
}
