package com.flowiq.controller;

import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.dto.response.AnalyticsOverviewResponse;
import com.flowiq.dto.response.CategoryAmountResponse;
import com.flowiq.dto.response.FopInsightsResponse;
import com.flowiq.dto.response.MonthlyAmountResponse;
import com.flowiq.dto.response.MonthlyComparisonResponse;
import com.flowiq.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;

@Tag(name = "Analytics", description = "Financial analytics, trends, and FOP insights")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Analytics overview", description = "Returns a high-level overview of revenue, expenses, and profit.")
    @ApiResponse(responseCode = "200", description = "Analytics overview",
            content = @Content(schema = @Schema(implementation = AnalyticsOverviewResponse.class)))
    @ApiErrorResponses
    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview() {
        return ResponseEntity.ok(analyticsService.getOverview());
    }

    @Operation(summary = "Revenue trend", description = "Returns monthly revenue amounts over time.")
    @ApiResponse(responseCode = "200", description = "Monthly revenue trend",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MonthlyAmountResponse.class))))
    @ApiErrorResponses
    @GetMapping("/revenue-trend")
    public ResponseEntity<List<MonthlyAmountResponse>> getRevenueTrend() {
        return ResponseEntity.ok(analyticsService.getRevenueTrend());
    }

    @Operation(summary = "Expense breakdown", description = "Returns expense totals grouped by category.")
    @ApiResponse(responseCode = "200", description = "Expense breakdown",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryAmountResponse.class))))
    @ApiErrorResponses
    @GetMapping("/expense-breakdown")
    public ResponseEntity<List<CategoryAmountResponse>> getExpenseBreakdown() {
        return ResponseEntity.ok(analyticsService.getExpenseBreakdown());
    }

    @Operation(summary = "Profit trend", description = "Returns monthly profit amounts over time.")
    @ApiResponse(responseCode = "200", description = "Monthly profit trend",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MonthlyAmountResponse.class))))
    @ApiErrorResponses
    @GetMapping("/profit-trend")
    public ResponseEntity<List<MonthlyAmountResponse>> getProfitTrend() {
        return ResponseEntity.ok(analyticsService.getProfitTrend());
    }

    @Operation(summary = "FOP insights", description = "Returns insights specific to Ukrainian FOP tax limits and compliance.")
    @ApiResponse(responseCode = "200", description = "FOP insights",
            content = @Content(schema = @Schema(implementation = FopInsightsResponse.class)))
    @ApiErrorResponses
    @GetMapping("/fop-insights")
    public ResponseEntity<FopInsightsResponse> getFopInsights() {
        return ResponseEntity.ok(analyticsService.getFopInsights());
    }

    @Operation(summary = "Income vs expenses", description = "Returns monthly comparison of income and expenses.")
    @ApiResponse(responseCode = "200", description = "Monthly income vs expenses",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MonthlyComparisonResponse.class))))
    @ApiErrorResponses
    @GetMapping("/income-vs-expenses")
    public ResponseEntity<List<MonthlyComparisonResponse>> getIncomeVsExpenses() {
        return ResponseEntity.ok(analyticsService.getIncomeVsExpenses());
    }
}
