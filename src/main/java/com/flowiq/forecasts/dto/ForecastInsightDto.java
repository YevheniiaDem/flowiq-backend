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
@Schema(description = "AI-generated forecast insight")
public class ForecastInsightDto {

    private String id;
    private String message;
    private ForecastSeverity severity;
    private String category;
}
