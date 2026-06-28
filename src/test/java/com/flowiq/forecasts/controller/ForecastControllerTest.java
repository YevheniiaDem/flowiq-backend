package com.flowiq.forecasts.controller;

import com.flowiq.exception.UnauthorizedException;
import com.flowiq.forecasts.dto.FopLimitForecastResponse;
import com.flowiq.forecasts.dto.ForecastMetricResponse;
import com.flowiq.forecasts.dto.ForecastSummaryResponse;
import com.flowiq.forecasts.dto.TaxForecastResponse;
import com.flowiq.forecasts.service.ForecastService;
import com.flowiq.unit.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForecastController tests")
class ForecastControllerTest {

    @Mock
    private ForecastService forecastService;

    @InjectMocks
    private ForecastController forecastController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.standaloneMockMvc(forecastController);
    }

    @Test
    @DisplayName("GET /api/forecasts/revenue returns revenue forecast")
    void revenue_success() throws Exception {
        when(forecastService.getRevenueForecast()).thenReturn(
                ForecastMetricResponse.builder().trendPercent(5.2).build());

        mockMvc.perform(get("/api/forecasts/revenue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trendPercent").value(5.2));
    }

    @Test
    @DisplayName("GET /api/forecasts/expenses returns expense forecast")
    void expenses_success() throws Exception {
        when(forecastService.getExpenseForecast()).thenReturn(
                ForecastMetricResponse.builder().trendPercent(-2.1).build());

        mockMvc.perform(get("/api/forecasts/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trendPercent").value(-2.1));
    }

    @Test
    @DisplayName("GET /api/forecasts/profit returns profit forecast")
    void profit_success() throws Exception {
        when(forecastService.getProfitForecast()).thenReturn(
                ForecastMetricResponse.builder().trendPercent(3.5).build());

        mockMvc.perform(get("/api/forecasts/profit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trendPercent").value(3.5));
    }

    @Test
    @DisplayName("GET /api/forecasts/taxes returns tax forecast")
    void taxes_success() throws Exception {
        when(forecastService.getTaxForecast()).thenReturn(
                TaxForecastResponse.builder().fopGroup(2).annualTaxForecast(new BigDecimal("12000")).build());

        mockMvc.perform(get("/api/forecasts/taxes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fopGroup").value(2));
    }

    @Test
    @DisplayName("GET /api/forecasts/fop-limit returns FOP limit forecast")
    void fopLimit_success() throws Exception {
        when(forecastService.getFopLimitForecast()).thenReturn(
                FopLimitForecastResponse.builder().fopGroup(2).currentUsagePercent(45.5).build());

        mockMvc.perform(get("/api/forecasts/fop-limit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentUsagePercent").value(45.5));
    }

    @Test
    @DisplayName("GET /api/forecasts/summary returns forecast summary")
    void summary_success() throws Exception {
        when(forecastService.getSummary()).thenReturn(
                ForecastSummaryResponse.builder()
                        .expectedRevenue(new BigDecimal("50000"))
                        .expectedProfit(new BigDecimal("15000"))
                        .insights(List.of())
                        .warnings(List.of())
                        .build());

        mockMvc.perform(get("/api/forecasts/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expectedProfit").value(15000));
    }

    @Test
    @DisplayName("GET /api/forecasts/revenue returns 401 when unauthorized")
    void revenue_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("Not authenticated")).when(forecastService).getRevenueForecast();

        mockMvc.perform(get("/api/forecasts/revenue"))
                .andExpect(status().isUnauthorized());
    }
}
