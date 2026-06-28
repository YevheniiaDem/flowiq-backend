package com.flowiq.controller;

import com.flowiq.dto.request.AIAccountantChatRequest;
import com.flowiq.dto.response.AIAccountantChatResponse;
import com.flowiq.dto.response.AIAccountantHealthResponse;
import com.flowiq.dto.response.AIRecommendationResponse;
import com.flowiq.dto.response.ForecastsResponse;
import com.flowiq.dto.response.TaxAdvisorResponse;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.service.AIAccountantService;
import com.flowiq.unit.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIAccountantController tests")
class AIAccountantControllerTest {

    @Mock
    private AIAccountantService aiAccountantService;

    @InjectMocks
    private AIAccountantController aiAccountantController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.standaloneMockMvc(aiAccountantController);
    }

    @Test
    @DisplayName("GET /api/ai-accountant/health returns health status")
    void health_success() throws Exception {
        when(aiAccountantService.getHealth()).thenReturn(
                AIAccountantHealthResponse.builder().score(95).status("healthy").build());

        mockMvc.perform(get("/api/ai-accountant/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(95))
                .andExpect(jsonPath("$.status").value("healthy"));
    }

    @Test
    @DisplayName("GET /api/ai-accountant/recommendations returns recommendations")
    void recommendations_success() throws Exception {
        when(aiAccountantService.getRecommendations()).thenReturn(List.of(
                AIRecommendationResponse.builder().id("1").title("Reduce expenses").build()));

        mockMvc.perform(get("/api/ai-accountant/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Reduce expenses"));
    }

    @Test
    @DisplayName("GET /api/ai-accountant/tax-advisor returns tax advice")
    void taxAdvisor_success() throws Exception {
        when(aiAccountantService.getTaxAdvisor()).thenReturn(
                TaxAdvisorResponse.builder().fopGroupNumber(2).estimatedTaxes(new BigDecimal("5000")).build());

        mockMvc.perform(get("/api/ai-accountant/tax-advisor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fopGroupNumber").value(2));
    }

    @Test
    @DisplayName("GET /api/ai-accountant/forecasts returns forecasts")
    void forecasts_success() throws Exception {
        when(aiAccountantService.getForecasts()).thenReturn(ForecastsResponse.builder().horizons(List.of()).build());

        mockMvc.perform(get("/api/ai-accountant/forecasts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horizons").isArray());
    }

    @Test
    @DisplayName("POST /api/ai-accountant/chat returns AI reply")
    void chat_success() throws Exception {
        AIAccountantChatRequest request = new AIAccountantChatRequest();
        request.setMessage("What are my top expenses?");

        when(aiAccountantService.chat(any(AIAccountantChatRequest.class)))
                .thenReturn(AIAccountantChatResponse.builder().reply("Your top expense is rent.").build());

        mockMvc.perform(post("/api/ai-accountant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Your top expense is rent."));
    }

    @Test
    @DisplayName("POST /api/ai-accountant/chat rejects blank message")
    void chat_validationError() throws Exception {
        AIAccountantChatRequest request = new AIAccountantChatRequest();
        request.setMessage("   ");

        mockMvc.perform(post("/api/ai-accountant/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/ai-accountant/health returns 401 when unauthorized")
    void health_unauthorized() throws Exception {
        doThrow(new UnauthorizedException("Not authenticated")).when(aiAccountantService).getHealth();

        mockMvc.perform(get("/api/ai-accountant/health"))
                .andExpect(status().isUnauthorized());
    }
}
