package com.flowiq.controller;

import com.flowiq.dto.response.AIInsightResponse;
import com.flowiq.dto.response.AISummaryResponse;
import com.flowiq.dto.response.BusinessHealthResponse;
import com.flowiq.dto.response.CategoryAmountResponse;
import com.flowiq.dto.response.MonthlyAmountResponse;
import com.flowiq.dto.response.StatCardResponse;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.forecasts.dto.ForecastSnapshotResponse;
import com.flowiq.forecasts.service.ForecastService;
import com.flowiq.knowledge.dto.KnowledgeDashboardSnapshotDto;
import com.flowiq.knowledge.service.KnowledgeService;
import com.flowiq.service.DashboardService;
import com.flowiq.tasks.dto.TaskSnapshotResponse;
import com.flowiq.tasks.service.TaskService;
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
@DisplayName("DashboardController tests")
class DashboardControllerTest {

    @Mock private DashboardService dashboardService;
    @Mock private ForecastService forecastService;
    @Mock private TaskService taskService;
    @Mock private KnowledgeService knowledgeService;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.standaloneMockMvc(dashboardController);
    }

    @Test
    @DisplayName("GET /api/dashboard/stats returns stat cards")
    void stats_success() throws Exception {
        when(dashboardService.getStats()).thenReturn(List.of(
                StatCardResponse.builder().labelKey("revenue").amount(new java.math.BigDecimal("1000")).build()));

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].labelKey").value("revenue"));
    }

    @Test
    @DisplayName("GET /api/dashboard/insights returns AI insights")
    void insights_success() throws Exception {
        when(dashboardService.getInsights()).thenReturn(List.of(
                AIInsightResponse.builder().id("ins-001").type("warning").build()));

        mockMvc.perform(get("/api/dashboard/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ins-001"));
    }

    @Test
    @DisplayName("GET /api/dashboard/health returns business health")
    void health_success() throws Exception {
        when(dashboardService.getBusinessHealth()).thenReturn(
                BusinessHealthResponse.builder().score(85).status("good").build());

        mockMvc.perform(get("/api/dashboard/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(85));
    }

    @Test
    @DisplayName("GET /api/dashboard/summary returns AI summary")
    void summary_success() throws Exception {
        when(dashboardService.getAISummary()).thenReturn(
                AISummaryResponse.builder().text("Strong month").build());

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Strong month"));
    }

    @Test
    @DisplayName("GET /api/dashboard/charts/revenue-trend returns trend data")
    void revenueTrend_success() throws Exception {
        when(dashboardService.getRevenueTrend()).thenReturn(List.of(
                MonthlyAmountResponse.builder().month("2026-06").amount(new java.math.BigDecimal("5000")).build()));

        mockMvc.perform(get("/api/dashboard/charts/revenue-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].month").value("2026-06"));
    }

    @Test
    @DisplayName("GET /api/dashboard/charts/expense-breakdown returns categories")
    void expenseBreakdown_success() throws Exception {
        when(dashboardService.getExpenseBreakdown()).thenReturn(List.of(
                CategoryAmountResponse.builder().category("Office").amount(new java.math.BigDecimal("300")).build()));

        mockMvc.perform(get("/api/dashboard/charts/expense-breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Office"));
    }

    @Test
    @DisplayName("GET /api/dashboard/forecast-snapshot returns forecast widget data")
    void forecastSnapshot_success() throws Exception {
        when(forecastService.getSnapshot()).thenReturn(ForecastSnapshotResponse.builder().build());

        mockMvc.perform(get("/api/dashboard/forecast-snapshot"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/dashboard/tasks-snapshot returns tasks widget data")
    void tasksSnapshot_success() throws Exception {
        when(taskService.getSnapshot()).thenReturn(TaskSnapshotResponse.builder().todayCount(2).build());

        mockMvc.perform(get("/api/dashboard/tasks-snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayCount").value(2));
    }

    @Test
    @DisplayName("GET /api/dashboard/business-guide-snapshot returns knowledge widget data")
    void businessGuideSnapshot_success() throws Exception {
        when(knowledgeService.getDashboardSnapshot()).thenReturn(KnowledgeDashboardSnapshotDto.builder().build());

        mockMvc.perform(get("/api/dashboard/business-guide-snapshot"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/dashboard/stats returns 401 when unauthorized")
    void stats_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("Not authenticated")).when(dashboardService).getStats();

        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }
}
