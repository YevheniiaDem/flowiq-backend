package com.flowiq.unit.service;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.profile.service.FopProfileService;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
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
@DisplayName("AnalyticsService unit tests")
class AnalyticsServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "analytics@test.flowiq";

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionSeedService transactionSeedService;
    @Mock
    private FopProfileService fopProfileService;

    private AnalyticsService analyticsService;
    private User user;

    @BeforeEach
    void setUp() {
        analyticsService = new AnalyticsService(
                transactionRepository,
                userRepository,
                transactionSeedService,
                fopProfileService,
                List.of()
        );
        user = SecurityTestSupport.testUser(USER_ID, EMAIL);
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(fopProfileService.resolveEffectiveFopGroup(anyLong(), any(BigDecimal.class))).thenReturn(2);
        when(fopProfileService.resolveEffectiveTaxRate(anyLong(), eq(2))).thenReturn(new BigDecimal("0.05"));
        when(fopProfileService.incomeLimitForGroup(2)).thenReturn(new BigDecimal("5328000"));
        stubZeroSums();
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("getOverview returns YTD metrics with tax burden")
    void getOverview_success() {
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("100000"));
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("40000"));

        var overview = analyticsService.getOverview();

        assertThat(overview.getRevenue()).isEqualByComparingTo("100000");
        assertThat(overview.getExpenses()).isEqualByComparingTo("40000");
        assertThat(overview.getProfit()).isEqualByComparingTo("60000");
        verify(transactionSeedService).seedIfEmpty(user);
        verify(fopProfileService, org.mockito.Mockito.atLeastOnce())
                .resolveEffectiveFopGroup(eq(USER_ID), any(BigDecimal.class));
    }

    @Test
    @DisplayName("getRevenueTrend returns twelve monthly points")
    void getRevenueTrend_success() {
        var trend = analyticsService.getRevenueTrend();

        assertThat(trend).hasSize(12);
    }

    @Test
    @DisplayName("getProfitTrend returns monthly profit series")
    void getProfitTrend_success() {
        var trend = analyticsService.getProfitTrend();

        assertThat(trend).hasSize(12);
        assertThat(trend.get(0).getAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("getIncomeVsExpenses returns comparison for twelve months")
    void getIncomeVsExpenses_success() {
        var comparison = analyticsService.getIncomeVsExpenses();

        assertThat(comparison).hasSize(12);
        assertThat(comparison.get(0).getMonth()).isEqualTo(YearMonth.now().minusMonths(11).toString());
    }

    @Test
    @DisplayName("getFopInsights returns FOP group and limit usage")
    void getFopInsights_success() {
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("500000"));

        var insights = analyticsService.getFopInsights();

        assertThat(insights.getFopGroupNumber()).isEqualTo(2);
        assertThat(insights.getAnnualIncome()).isEqualByComparingTo("500000");
        assertThat(insights.getIncomeLimit()).isEqualByComparingTo("5328000");
        assertThat(insights.getDaysUntilNextTaxPayment()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("getExpenseBreakdown maps categories")
    void getExpenseBreakdown_success() {
        when(transactionRepository.sumExpensesByCategory(eq(USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(categorySum("Salaries", "8000")));

        var breakdown = analyticsService.getExpenseBreakdown();

        assertThat(breakdown).hasSize(1);
        assertThat(breakdown.get(0).getCategory()).isEqualTo("Salaries");
    }

    @Test
    @DisplayName("rejects unauthenticated access")
    void rejectsUnauthenticated() {
        SecurityTestSupport.clearSecurityContext();

        assertThatThrownBy(() -> analyticsService.getOverview())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Not authenticated");
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
