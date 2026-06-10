package com.flowiq.controller;

import com.flowiq.dto.response.AnalyticsOverviewResponse;
import com.flowiq.dto.response.CategoryAmountResponse;
import com.flowiq.dto.response.FopInsightsResponse;
import com.flowiq.dto.response.MonthlyAmountResponse;
import com.flowiq.dto.response.MonthlyComparisonResponse;
import com.flowiq.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview() {
        return ResponseEntity.ok(analyticsService.getOverview());
    }

    @GetMapping("/revenue-trend")
    public ResponseEntity<List<MonthlyAmountResponse>> getRevenueTrend() {
        return ResponseEntity.ok(analyticsService.getRevenueTrend());
    }

    @GetMapping("/expense-breakdown")
    public ResponseEntity<List<CategoryAmountResponse>> getExpenseBreakdown() {
        return ResponseEntity.ok(analyticsService.getExpenseBreakdown());
    }

    @GetMapping("/profit-trend")
    public ResponseEntity<List<MonthlyAmountResponse>> getProfitTrend() {
        return ResponseEntity.ok(analyticsService.getProfitTrend());
    }

    @GetMapping("/fop-insights")
    public ResponseEntity<FopInsightsResponse> getFopInsights() {
        return ResponseEntity.ok(analyticsService.getFopInsights());
    }

    @GetMapping("/income-vs-expenses")
    public ResponseEntity<List<MonthlyComparisonResponse>> getIncomeVsExpenses() {
        return ResponseEntity.ok(analyticsService.getIncomeVsExpenses());
    }
}
