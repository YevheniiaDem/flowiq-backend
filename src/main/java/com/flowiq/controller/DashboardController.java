package com.flowiq.controller;

import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.dto.response.AIInsightResponse;
import com.flowiq.dto.response.AISummaryResponse;
import com.flowiq.dto.response.BusinessHealthResponse;
import com.flowiq.dto.response.CategoryAmountResponse;
import com.flowiq.dto.response.MonthlyAmountResponse;
import com.flowiq.dto.response.StatCardResponse;
import com.flowiq.forecasts.dto.ForecastSnapshotResponse;
import com.flowiq.forecasts.service.ForecastService;
import com.flowiq.service.DashboardService;
import com.flowiq.knowledge.dto.KnowledgeDashboardSnapshotDto;
import com.flowiq.knowledge.service.KnowledgeService;
import com.flowiq.tasks.dto.TaskSnapshotResponse;
import com.flowiq.tasks.service.TaskService;
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

@Tag(name = "Dashboard", description = "Business dashboard statistics, insights, and charts")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class DashboardController {

    private final DashboardService dashboardService;
    private final ForecastService forecastService;
    private final TaskService taskService;
    private final KnowledgeService knowledgeService;

    @Operation(summary = "Dashboard stat cards", description = "Returns key financial stat cards for the dashboard overview.")
    @ApiResponse(responseCode = "200", description = "List of stat cards",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = StatCardResponse.class))))
    @ApiErrorResponses
    @GetMapping("/stats")
    public ResponseEntity<List<StatCardResponse>> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @Operation(summary = "AI insights", description = "Returns AI-generated business insights for the dashboard.")
    @ApiResponse(responseCode = "200", description = "List of AI insights",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AIInsightResponse.class))))
    @ApiErrorResponses
    @GetMapping("/insights")
    public ResponseEntity<List<AIInsightResponse>> getInsights() {
        return ResponseEntity.ok(dashboardService.getInsights());
    }

    @Operation(summary = "Business health score", description = "Returns an overall business health assessment.")
    @ApiResponse(responseCode = "200", description = "Business health data",
            content = @Content(schema = @Schema(implementation = BusinessHealthResponse.class)))
    @ApiErrorResponses
    @GetMapping("/health")
    public ResponseEntity<BusinessHealthResponse> getBusinessHealth() {
        return ResponseEntity.ok(dashboardService.getBusinessHealth());
    }

    @Operation(summary = "AI summary", description = "Returns a natural-language AI summary of business performance.")
    @ApiResponse(responseCode = "200", description = "AI summary",
            content = @Content(schema = @Schema(implementation = AISummaryResponse.class)))
    @ApiErrorResponses
    @GetMapping("/summary")
    public ResponseEntity<AISummaryResponse> getAISummary() {
        return ResponseEntity.ok(dashboardService.getAISummary());
    }

    @Operation(summary = "Revenue trend chart", description = "Returns monthly revenue data for dashboard charts.")
    @ApiResponse(responseCode = "200", description = "Monthly revenue trend",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = MonthlyAmountResponse.class))))
    @ApiErrorResponses
    @GetMapping("/charts/revenue-trend")
    public ResponseEntity<List<MonthlyAmountResponse>> getRevenueTrend() {
        return ResponseEntity.ok(dashboardService.getRevenueTrend());
    }

    @Operation(summary = "Expense breakdown chart", description = "Returns expense amounts grouped by category.")
    @ApiResponse(responseCode = "200", description = "Expense breakdown by category",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = CategoryAmountResponse.class))))
    @ApiErrorResponses
    @GetMapping("/charts/expense-breakdown")
    public ResponseEntity<List<CategoryAmountResponse>> getExpenseBreakdown() {
        return ResponseEntity.ok(dashboardService.getExpenseBreakdown());
    }

    @Operation(summary = "Forecast snapshot", description = "Returns a compact forecast summary for the dashboard widget.")
    @ApiResponse(responseCode = "200", description = "Forecast snapshot",
            content = @Content(schema = @Schema(implementation = ForecastSnapshotResponse.class)))
    @ApiErrorResponses
    @GetMapping("/forecast-snapshot")
    public ResponseEntity<ForecastSnapshotResponse> getForecastSnapshot() {
        return ResponseEntity.ok(forecastService.getSnapshot());
    }

    @Operation(summary = "Tasks snapshot", description = "Returns today's tasks and upcoming deadlines for the dashboard widget.")
    @ApiResponse(responseCode = "200", description = "Tasks snapshot",
            content = @Content(schema = @Schema(implementation = TaskSnapshotResponse.class)))
    @ApiErrorResponses
    @GetMapping("/tasks-snapshot")
    public ResponseEntity<TaskSnapshotResponse> getTasksSnapshot() {
        return ResponseEntity.ok(taskService.getSnapshot());
    }

    @Operation(summary = "Business Guide snapshot", description = "Returns popular, recent, and recommended knowledge articles for the dashboard widget.")
    @ApiResponse(responseCode = "200", description = "Business Guide snapshot",
            content = @Content(schema = @Schema(implementation = KnowledgeDashboardSnapshotDto.class)))
    @ApiErrorResponses
    @GetMapping("/business-guide-snapshot")
    public ResponseEntity<KnowledgeDashboardSnapshotDto> getBusinessGuideSnapshot() {
        return ResponseEntity.ok(knowledgeService.getDashboardSnapshot());
    }
}
