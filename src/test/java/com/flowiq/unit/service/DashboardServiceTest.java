package com.flowiq.unit.service;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.service.DashboardService;
import com.flowiq.service.TransactionSeedService;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DashboardService unit tests")
class DashboardServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "dashboard@test.flowiq";

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionSeedService transactionSeedService;

    @InjectMocks
    private DashboardService dashboardService;

    private User user;

    @BeforeEach
    void setUp() {
        user = SecurityTestSupport.testUser(USER_ID, EMAIL);
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        stubZeroSums();
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("getStats returns four stat cards and seeds transactions")
    void getStats_success() {
        YearMonth current = YearMonth.now();
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("10000"));
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("4000"));

        var stats = dashboardService.getStats();

        assertThat(stats).hasSize(4);
        assertThat(stats.get(0).getLabelKey()).isEqualTo("revenue");
        assertThat(stats.get(0).getAmount()).isEqualByComparingTo("10000");
        verify(transactionSeedService).seedIfEmpty(user);
    }

    @Test
    @DisplayName("getInsights returns default insight when no growth signals")
    void getInsights_defaultInsight() {
        var insights = dashboardService.getInsights();

        assertThat(insights).isNotEmpty();
        assertThat(insights.get(0).getId()).isEqualTo("ins-default");
    }

    @Test
    @DisplayName("getInsights returns warning when expenses grow faster than revenue")
    void getInsights_expenseWarning() {
        YearMonth current = YearMonth.now();
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    LocalDate start = inv.getArgument(2);
                    return start.getMonthValue() == current.getMonthValue()
                            && start.getYear() == current.getYear()
                            ? new BigDecimal("1000") : new BigDecimal("900");
                });
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    LocalDate start = inv.getArgument(2);
                    return start.getMonthValue() == current.getMonthValue()
                            && start.getYear() == current.getYear()
                            ? new BigDecimal("2000") : new BigDecimal("1000");
                });

        var insights = dashboardService.getInsights();

        assertThat(insights).anyMatch(i -> "ins-001".equals(i.getId()));
    }

    @Test
    @DisplayName("getBusinessHealth returns excellent status for high margin")
    void getBusinessHealth_excellent() {
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("100000"));
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("10000"));

        var health = dashboardService.getBusinessHealth();

        assertThat(health.getStatus()).isEqualTo("excellent");
    }

    @Test
    @DisplayName("getBusinessHealth returns score for current month")
    void getBusinessHealth_success() {
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("50000"));
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("20000"));

        var health = dashboardService.getBusinessHealth();

        assertThat(health.getScore()).isBetween(0, 100);
        assertThat(health.getMaxScore()).isEqualTo(100);
        assertThat(health.getStatus()).isNotBlank();
    }

    @Test
    @DisplayName("getRevenueTrend returns six monthly data points")
    void getRevenueTrend_success() {
        var trend = dashboardService.getRevenueTrend();

        assertThat(trend).hasSize(6);
        assertThat(trend.get(0).getMonth()).isNotBlank();
    }

    @Test
    @DisplayName("getExpenseBreakdown maps category sums")
    void getExpenseBreakdown_success() {
        when(transactionRepository.sumExpensesByCategory(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(categorySum("Marketing", "3000")));

        var breakdown = dashboardService.getExpenseBreakdown();

        assertThat(breakdown).hasSize(1);
        assertThat(breakdown.get(0).getCategory()).isEqualTo("Marketing");
    }

    @Test
    @DisplayName("getAISummary returns summary text")
    void getAISummary_success() {
        var summary = dashboardService.getAISummary();

        assertThat(summary.getText()).isNotBlank();
        assertThat(summary.getBadge()).isNotBlank();
    }

    @Test
    @DisplayName("rejects unauthenticated access")
    void rejectsUnauthenticated() {
        SecurityTestSupport.clearSecurityContext();

        assertThatThrownBy(() -> dashboardService.getStats())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Not authenticated");
    }

    @Test
    @DisplayName("rejects when authenticated user not found in database")
    void rejectsMissingUser() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dashboardService.getStats())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User not found");
    }

    private void stubZeroSums() {
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                anyLong(), any(Transaction.Type.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumExpensesByCategory(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
    }

    private TransactionRepository.CategorySumProjection categorySum(String category, String amount) {
        return new TransactionRepository.CategorySumProjection() {
            @Override
            public String getCategory() {
                return category;
            }

            @Override
            public BigDecimal getAmount() {
                return new BigDecimal(amount);
            }
        };
    }
}
