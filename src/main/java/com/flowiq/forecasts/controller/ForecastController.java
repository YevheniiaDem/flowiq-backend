package com.flowiq.forecasts.controller;

import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.forecasts.dto.*;
import com.flowiq.forecasts.service.ForecastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Forecasts", description = "Forecast Center 2.0 — revenue, expenses, profit, tax, and FOP limit predictions")
@RestController
@RequestMapping("/api/forecasts")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ForecastController {

    private final ForecastService forecastService;

    @Operation(summary = "Revenue forecast", description = "Returns historical and projected revenue with trend analysis.")
    @ApiResponse(responseCode = "200", description = "Revenue forecast",
            content = @Content(schema = @Schema(implementation = ForecastMetricResponse.class)))
    @ApiErrorResponses
    @GetMapping("/revenue")
    public ResponseEntity<ForecastMetricResponse> getRevenueForecast() {
        return ResponseEntity.ok(forecastService.getRevenueForecast());
    }

    @Operation(summary = "Expense forecast", description = "Returns historical and projected expenses with trend analysis.")
    @ApiResponse(responseCode = "200", description = "Expense forecast",
            content = @Content(schema = @Schema(implementation = ForecastMetricResponse.class)))
    @ApiErrorResponses
    @GetMapping("/expenses")
    public ResponseEntity<ForecastMetricResponse> getExpenseForecast() {
        return ResponseEntity.ok(forecastService.getExpenseForecast());
    }

    @Operation(summary = "Profit forecast", description = "Returns historical and projected profit with trend analysis.")
    @ApiResponse(responseCode = "200", description = "Profit forecast",
            content = @Content(schema = @Schema(implementation = ForecastMetricResponse.class)))
    @ApiErrorResponses
    @GetMapping("/profit")
    public ResponseEntity<ForecastMetricResponse> getProfitForecast() {
        return ResponseEntity.ok(forecastService.getProfitForecast());
    }

    @Operation(summary = "Tax forecast", description = "Returns projected tax burden based on revenue forecasts.")
    @ApiResponse(responseCode = "200", description = "Tax forecast",
            content = @Content(schema = @Schema(implementation = TaxForecastResponse.class)))
    @ApiErrorResponses
    @GetMapping("/taxes")
    public ResponseEntity<TaxForecastResponse> getTaxForecast() {
        return ResponseEntity.ok(forecastService.getTaxForecast());
    }

    @Operation(summary = "FOP limit forecast", description = "Returns projected FOP income limit usage.")
    @ApiResponse(responseCode = "200", description = "FOP limit forecast",
            content = @Content(schema = @Schema(implementation = FopLimitForecastResponse.class)))
    @ApiErrorResponses
    @GetMapping("/fop-limit")
    public ResponseEntity<FopLimitForecastResponse> getFopLimitForecast() {
        return ResponseEntity.ok(forecastService.getFopLimitForecast());
    }

    @Operation(summary = "Forecast summary", description = "Returns complete forecast summary with AI insights and warnings.")
    @ApiResponse(responseCode = "200", description = "Forecast summary",
            content = @Content(schema = @Schema(implementation = ForecastSummaryResponse.class)))
    @ApiErrorResponses
    @GetMapping("/summary")
    public ResponseEntity<ForecastSummaryResponse> getSummary() {
        return ResponseEntity.ok(forecastService.getSummary());
    }
}
