package com.flowiq.controller;

import com.flowiq.dto.response.AnalyticsOverviewResponse;
import com.flowiq.dto.response.CategoryAmountResponse;
import com.flowiq.dto.response.FopInsightsResponse;
import com.flowiq.dto.response.MonthlyAmountResponse;
import com.flowiq.dto.response.MonthlyComparisonResponse;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.service.AnalyticsService;
import com.flowiq.unit.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsController tests")
class AnalyticsControllerTest {

    @Mock private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController analyticsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.standaloneMockMvc(analyticsController);
    }

    @Test
    @DisplayName("GET /api/analytics/overview returns overview")
    void overview_success() throws Exception {
        when(analyticsService.getOverview()).thenReturn(
                AnalyticsOverviewResponse.builder().revenue(new java.math.BigDecimal("10000")).build());

        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revenue").value(10000));
    }

    @Test
    @DisplayName("GET /api/analytics/revenue-trend returns monthly revenue")
    void revenueTrend_success() throws Exception {
        when(analyticsService.getRevenueTrend()).thenReturn(List.of(
                MonthlyAmountResponse.builder().month("2026-01").amount(new java.math.BigDecimal("1000")).build()));

        mockMvc.perform(get("/api/analytics/revenue-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("2026-01"));
    }

    @Test
    @DisplayName("GET /api/analytics/expense-breakdown returns categories")
    void expenseBreakdown_success() throws Exception {
        when(analyticsService.getExpenseBreakdown()).thenReturn(List.of(
                CategoryAmountResponse.builder().category("Office").amount(new java.math.BigDecimal("200")).build()));

        mockMvc.perform(get("/api/analytics/expense-breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Office"));
    }

    @Test
    @DisplayName("GET /api/analytics/profit-trend returns monthly profit")
    void profitTrend_success() throws Exception {
        when(analyticsService.getProfitTrend()).thenReturn(List.of(
                MonthlyAmountResponse.builder().month("2026-06").amount(new java.math.BigDecimal("500")).build()));

        mockMvc.perform(get("/api/analytics/profit-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(500));
    }

    @Test
    @DisplayName("GET /api/analytics/fop-insights returns FOP insights")
    void fopInsights_success() throws Exception {
        when(analyticsService.getFopInsights()).thenReturn(
                FopInsightsResponse.builder().fopGroupNumber(2).build());

        mockMvc.perform(get("/api/analytics/fop-insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fopGroupNumber").value(2));
    }

    @Test
    @DisplayName("GET /api/analytics/income-vs-expenses returns comparison")
    void incomeVsExpenses_success() throws Exception {
        when(analyticsService.getIncomeVsExpenses()).thenReturn(List.of(
                MonthlyComparisonResponse.builder()
                        .month("2026-06")
                        .revenue(new java.math.BigDecimal("3000"))
                        .expenses(new java.math.BigDecimal("1500"))
                        .build()));

        mockMvc.perform(get("/api/analytics/income-vs-expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].revenue").value(3000));
    }

    @Test
    @DisplayName("GET /api/analytics/overview returns 401 when unauthorized")
    void overview_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("Not authenticated")).when(analyticsService).getOverview();

        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isUnauthorized());
    }
}
