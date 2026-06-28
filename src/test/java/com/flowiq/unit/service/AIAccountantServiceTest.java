package com.flowiq.unit.service;

import com.flowiq.aiaccountant.AIRecommendationEngine;
import com.flowiq.dto.request.AIAccountantChatRequest;
import com.flowiq.dto.response.AIRecommendationResponse;
import com.flowiq.dto.response.FopInsightsResponse;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.service.AIAccountantService;
import com.flowiq.service.AnalyticsService;
import com.flowiq.service.TransactionSeedService;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AIAccountantService unit tests")
class AIAccountantServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "ai@test.flowiq";

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionSeedService transactionSeedService;
    @Mock
    private AnalyticsService analyticsService;
    @Mock
    private AIRecommendationEngine recommendationEngine;

    private AIAccountantService aiAccountantService;
    private User user;

    @BeforeEach
    void setUp() {
        aiAccountantService = new AIAccountantService(
                transactionRepository,
                userRepository,
                transactionSeedService,
                analyticsService,
                recommendationEngine,
                List.of()
        );
        user = SecurityTestSupport.testUser(USER_ID, EMAIL);
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(analyticsService.getFopInsights()).thenReturn(sampleFopInsights());
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                anyLong(), any(Transaction.Type.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("50000"));
        when(transactionRepository.sumExpensesByCategory(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(recommendationEngine.generate(any())).thenReturn(List.of(
                AIRecommendationResponse.builder().id("rec-stable").type("SUCCESS").build()
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("getHealth returns score and highlights")
    void getHealth_success() {
        var health = aiAccountantService.getHealth();

        assertThat(health.getScore()).isBetween(0, 100);
        assertThat(health.getStatus()).isNotBlank();
        assertThat(health.getHighlights()).isNotEmpty();
        verify(transactionSeedService).seedIfEmpty(user);
    }

    @Test
    @DisplayName("getRecommendations delegates to recommendation engine")
    void getRecommendations_success() {
        var recommendations = aiAccountantService.getRecommendations();

        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).getId()).isEqualTo("rec-stable");
        verify(recommendationEngine).generate(any());
    }

    @Test
    @DisplayName("getTaxAdvisor maps FOP insights")
    void getTaxAdvisor_success() {
        var taxAdvisor = aiAccountantService.getTaxAdvisor();

        assertThat(taxAdvisor.getFopGroupNumber()).isEqualTo(2);
        assertThat(taxAdvisor.getCurrentFopGroup()).isEqualTo("FOP Group 2");
        assertThat(taxAdvisor.getAnnualIncome()).isEqualByComparingTo("500000");
        verify(analyticsService, org.mockito.Mockito.atLeastOnce()).getFopInsights();
    }

    @Test
    @DisplayName("getForecasts returns three horizons")
    void getForecasts_success() {
        var forecasts = aiAccountantService.getForecasts();

        assertThat(forecasts.getHorizons()).hasSize(3);
        assertThat(forecasts.getHorizons().get(0).getMonths()).isEqualTo(3);
    }

    @Test
    @DisplayName("chat returns revenue-focused reply")
    void chat_revenueQuestion() {
        AIAccountantChatRequest request = new AIAccountantChatRequest();
        request.setMessage("What is my revenue?");

        var response = aiAccountantService.chat(request);

        assertThat(response.getReply()).isNotBlank();
    }

    @Test
    @DisplayName("rejects unauthenticated access")
    void rejectsUnauthenticated() {
        SecurityTestSupport.clearSecurityContext();

        assertThatThrownBy(() -> aiAccountantService.getHealth())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Not authenticated");
    }

    private FopInsightsResponse sampleFopInsights() {
        return FopInsightsResponse.builder()
                .currentFopGroup("FOP Group 2")
                .fopGroupNumber(2)
                .annualIncome(new BigDecimal("500000"))
                .incomeLimit(new BigDecimal("5328000"))
                .incomeLimitUsagePercent(9.4)
                .estimatedTaxLoad(new BigDecimal("35000"))
                .taxForecast(new BigDecimal("42000"))
                .daysUntilNextTaxPayment(30)
                .nextTaxPaymentLabel("Due by May 10 2026")
                .topExpenseCategories(List.of())
                .build();
    }
}
