package com.flowiq.controller;

import com.flowiq.dto.response.AIInsightResponse;
import com.flowiq.dto.response.AISummaryResponse;
import com.flowiq.dto.response.BusinessHealthResponse;
import com.flowiq.dto.response.CategoryAmountResponse;
import com.flowiq.dto.response.MonthlyAmountResponse;
import com.flowiq.dto.response.StatCardResponse;
import com.flowiq.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<List<StatCardResponse>> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/insights")
    public ResponseEntity<List<AIInsightResponse>> getInsights() {
        return ResponseEntity.ok(dashboardService.getInsights());
    }

    @GetMapping("/health")
    public ResponseEntity<BusinessHealthResponse> getBusinessHealth() {
        return ResponseEntity.ok(dashboardService.getBusinessHealth());
    }

    @GetMapping("/summary")
    public ResponseEntity<AISummaryResponse> getAISummary() {
        return ResponseEntity.ok(dashboardService.getAISummary());
    }

    @GetMapping("/charts/revenue-trend")
    public ResponseEntity<List<MonthlyAmountResponse>> getRevenueTrend() {
        return ResponseEntity.ok(dashboardService.getRevenueTrend());
    }

    @GetMapping("/charts/expense-breakdown")
    public ResponseEntity<List<CategoryAmountResponse>> getExpenseBreakdown() {
        return ResponseEntity.ok(dashboardService.getExpenseBreakdown());
    }
}
