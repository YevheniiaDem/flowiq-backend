package com.flowiq.unit.forecasts;

import com.flowiq.config.AppPreferences;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.forecasts.dto.*;
import com.flowiq.forecasts.engine.ForecastEngine;
import com.flowiq.forecasts.provider.RuleBasedForecastProvider;
import com.flowiq.forecasts.service.ForecastService;
import com.flowiq.profile.service.FopProfileService;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ForecastService unit tests")
class ForecastServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "forecast@test.flowiq";

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionSeedService transactionSeedService;
    @Mock
    private FopProfileService fopProfileService;

    private final ForecastEngine forecastEngine = new ForecastEngine();
    private final RuleBasedForecastProvider ruleBasedProvider = new RuleBasedForecastProvider();

    private ForecastService forecastService;

    private User user;

    @BeforeEach
    void setUp() {
        forecastService = new ForecastService(
                transactionRepository,
                userRepository,
                transactionSeedService,
                fopProfileService,
                forecastEngine,
                ruleBasedProvider
        );
        AppPreferences.clear();
        user = SecurityTestSupport.testUser(USER_ID, EMAIL);
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(fopProfileService.resolveEffectiveFopGroup(eq(USER_ID), any(BigDecimal.class)))
                .thenAnswer(invocation -> deriveFopGroup(invocation.getArgument(1)));
        stubMonthlyTransactions(new BigDecimal("10000"), new BigDecimal("4000"));
        stubYtdRevenue(new BigDecimal("200000"));
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
        AppPreferences.clear();
    }

    private static int deriveFopGroup(BigDecimal annualIncome) {
        if (annualIncome == null || annualIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return 2;
        }
        if (annualIncome.compareTo(new BigDecimal("1672000")) <= 0) {
            return 1;
        }
        if (annualIncome.compareTo(new BigDecimal("5328000")) <= 0) {
            return 2;
        }
        if (annualIncome.compareTo(new BigDecimal("7818000")) <= 0) {
            return 3;
        }
        return 0;
    }

    @Test
    @DisplayName("getRevenueForecast returns historical and projected data points")
    void getRevenueForecast_happyPath() {
        ForecastMetricResponse response = forecastService.getRevenueForecast();

        assertThat(response.getHistorical()).hasSize(12);
        assertThat(response.getProjected()).hasSize(12);
        assertThat(response.getHorizons()).hasSize(ForecastEngine.FORECAST_HORIZONS.length);
        verify(transactionSeedService).seedIfEmpty(user);
    }

    @Test
    @DisplayName("getExpenseForecast returns expense amounts")
    void getExpenseForecast_happyPath() {
        ForecastMetricResponse response = forecastService.getExpenseForecast();

        assertThat(response.getHistorical()).allMatch(p -> p.getAmount().compareTo(BigDecimal.ZERO) >= 0);
        assertThat(response.getProjected()).isNotEmpty();
    }

    @Test
    @DisplayName("getProfitForecast derives profit from revenue minus expenses")
    void getProfitForecast_calculatesProfit() {
        ForecastMetricResponse response = forecastService.getProfitForecast();

        assertThat(response.getHistorical().get(0).getAmount())
                .isEqualByComparingTo(new BigDecimal("6000"));
    }

    @Test
    @DisplayName("getTaxForecast includes FOP group and horizon cards")
    void getTaxForecast_includesTaxCards() {
        TaxForecastResponse response = forecastService.getTaxForecast();

        assertThat(response.getFopGroup()).isBetween(1, 3);
        assertThat(response.getCards()).hasSize(ForecastEngine.FORECAST_HORIZONS.length);
        assertThat(response.getCurrentTaxBurden()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("getFopLimitForecast calculates usage percent")
    void getFopLimitForecast_usagePercent() {
        FopLimitForecastResponse response = forecastService.getFopLimitForecast();

        assertThat(response.getIncomeLimit()).isGreaterThan(BigDecimal.ZERO);
        assertThat(response.getCurrentUsagePercent()).isBetween(0.0, 100.0);
        assertThat(response.getHorizons()).hasSize(ForecastEngine.FORECAST_HORIZONS.length);
    }

    @Test
    @DisplayName("getSummary aggregates expected metrics for 3-month horizon")
    void getSummary_threeMonthHorizon() {
        ForecastSummaryResponse response = forecastService.getSummary();

        assertThat(response.getExpectedRevenue()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(response.getExpectedExpenses()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        assertThat(response.getExpectedProfit()).isNotNull();
        assertThat(response.getInsights()).isNotNull();
        assertThat(response.getWarnings()).isNotNull();
    }

    @Test
    @DisplayName("getSnapshot returns compact forecast metrics")
    void getSnapshot_compactMetrics() {
        ForecastSnapshotResponse response = forecastService.getSnapshot();

        assertThat(response.getForecastMonths()).isEqualTo(3);
        assertThat(response.getExpectedRevenue()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("high YTD revenue resolves to FOP group 2 or 3")
    void getFopLimitForecast_highIncome_higherFopGroup() {
        stubYtdRevenue(new BigDecimal("3000000"));

        FopLimitForecastResponse response = forecastService.getFopLimitForecast();

        assertThat(response.getFopGroup()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("zero transactions still produce valid forecast structure")
    void getRevenueForecast_zeroTransactions() {
        stubMonthlyTransactions(BigDecimal.ZERO, BigDecimal.ZERO);
        stubYtdRevenue(BigDecimal.ZERO);

        ForecastMetricResponse response = forecastService.getRevenueForecast();

        assertThat(response.getHistorical()).hasSize(12);
        assertThat(response.getProjected()).hasSize(12);
    }

    @Test
    @DisplayName("Ukrainian locale uses Ukrainian FOP group label")
    void getFopLimitForecast_ukrainianLocale() {
        AppPreferences prefs = new AppPreferences();
        prefs.setLanguage("uk");
        AppPreferences.set(prefs);

        FopLimitForecastResponse response = forecastService.getFopLimitForecast();

        assertThat(response.getFopGroupLabel()).contains("ФОП");
    }

    @Test
    @DisplayName("throws UnauthorizedException when not authenticated")
    void getRevenueForecast_unauthenticated() {
        SecurityTestSupport.clearSecurityContext();

        assertThatThrownBy(() -> forecastService.getRevenueForecast())
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("throws UnauthorizedException when user not found in repository")
    void getRevenueForecast_userNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> forecastService.getRevenueForecast())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User not found");
    }

    private void stubMonthlyTransactions(BigDecimal revenue, BigDecimal expenses) {
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(revenue);
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(expenses);
    }

    private void stubYtdRevenue(BigDecimal ytdRevenue) {
        LocalDate yearStart = LocalDate.of(LocalDate.now().getYear(), 1, 1);
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), eq(yearStart), any(LocalDate.class)))
                .thenReturn(ytdRevenue);
    }
}
