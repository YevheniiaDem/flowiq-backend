package com.flowiq.unit.tasks;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.preferences.NotificationPreferenceKey;
import com.flowiq.profile.service.FopProfileService;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskType;
import com.flowiq.tasks.service.TaskGeneratorService;
import com.flowiq.tasks.service.TaskRuleEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskRuleEngine unit tests")
class TaskRuleEngineTest {

    private static final Long USER_ID = 7L;

    @Mock
    private TaskGeneratorService taskGenerator;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private FopProfileService fopProfileService;

    @InjectMocks
    private TaskRuleEngine engine;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                anyLong(), any(Transaction.Type.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(fopProfileService.resolveEffectiveFopGroup(anyLong(), any(BigDecimal.class)))
                .thenAnswer(invocation -> deriveFopGroup(invocation.getArgument(1)));
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
    @DisplayName("creates tax payment tasks when deadline is within 30 days")
    void generateForUser_taxDeadlineSoon_createsTaxTasks() {
        LocalDate today = LocalDate.of(2026, 4, 15);
        LocalDate deadline = LocalDate.of(2026, 5, 10);
        try (MockedStatic<LocalDate> localDate = org.mockito.Mockito.mockStatic(LocalDate.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            localDate.when(LocalDate::now).thenReturn(today);
            stubYtdRevenue(new BigDecimal("100000"), today);

            engine.generateForUser(user);

            verify(taskGenerator).createIfAbsent(
                    eq(USER_ID),
                    eq("tax-payment-" + deadline),
                    eq("Сплатити єдиний податок"),
                    contains("Дедлайн"),
                    eq(TaskType.TAX),
                    any(TaskPriority.class),
                    eq(deadline),
                    anyBoolean(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(NotificationPreferenceKey.class)
            );
        }
    }

    @Test
    @DisplayName("creates quarter declaration task when deadline is within 14 days")
    void generateForUser_declarationDeadlineSoon_createsReportingTask() {
        LocalDate today = LocalDate.of(2026, 4, 28);
        LocalDate deadline = LocalDate.of(2026, 5, 10);
        try (MockedStatic<LocalDate> localDate = org.mockito.Mockito.mockStatic(LocalDate.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            localDate.when(LocalDate::now).thenReturn(today);
            stubYtdRevenue(new BigDecimal("100000"), today);

            engine.generateForUser(user);

            verify(taskGenerator).createIfAbsent(
                    eq(USER_ID),
                    eq("quarter-declaration-" + deadline),
                    eq("Подати квартальну декларацію"),
                    anyString(),
                    eq(TaskType.REPORTING),
                    any(TaskPriority.class),
                    any(LocalDate.class),
                    anyBoolean(),
                    anyString(),
                    anyString(),
                eq(NotificationType.TAX),
                eq(NotificationSeverity.WARNING),
                any(NotificationPreferenceKey.class)
            );
        }
    }

    @Test
    @DisplayName("creates annual declaration task in Q4 when deadline is within 60 days")
    void generateForUser_q4AnnualDeclaration_createsTask() {
        LocalDate today = LocalDate.of(2026, 12, 15);
        try (MockedStatic<LocalDate> localDate = org.mockito.Mockito.mockStatic(LocalDate.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            localDate.when(LocalDate::now).thenReturn(today);
            stubYtdRevenue(new BigDecimal("100000"), today);

            engine.generateForUser(user);

            verify(taskGenerator).createIfAbsent(
                    eq(USER_ID),
                    contains("annual-declaration"),
                    eq("Підготувати річну декларацію"),
                    contains("річну"),
                    eq(TaskType.REPORTING),
                    any(TaskPriority.class),
                    any(LocalDate.class),
                    anyBoolean(),
                    eq("Річна декларація"),
                    anyString(),
                eq(NotificationType.TAX),
                eq(NotificationSeverity.INFO),
                any(NotificationPreferenceKey.class)
            );
        }
    }

    @Test
    @DisplayName("creates FOP limit review task when usage exceeds 70%")
    void generateForUser_highFopUsage_createsReviewTask() {
        stubYtdRevenue(new BigDecimal("1300000"));

        engine.generateForUser(user);

        verify(taskGenerator).createIfAbsent(
                eq(USER_ID),
                contains("fop-limit-review"),
                eq("Переглянути ліміт ФОП"),
                contains("Використано"),
                eq(TaskType.BUSINESS),
                any(TaskPriority.class),
                any(LocalDate.class),
                anyBoolean(),
                eq("Ліміт ФОП"),
                anyString(),
                eq(NotificationType.FOP_LIMIT),
                any(NotificationSeverity.class),
                any(NotificationPreferenceKey.class)
        );
    }

    @Test
    @DisplayName("skips FOP review task when usage below 70%")
    void generateForUser_lowFopUsage_noReviewTask() {
        stubYtdRevenue(new BigDecimal("500000"));

        engine.generateForUser(user);

        verify(taskGenerator, never()).createIfAbsent(
                eq(USER_ID),
                contains("fop-limit-review"),
                anyString(),
                anyString(),
                any(),
                any(),
                any(),
                anyBoolean(),
                any(),
                any(),
                any(),
                any(),
                any(NotificationPreferenceKey.class)
        );
    }

    @Test
    @DisplayName("creates expense growth task when expenses outpace revenue")
    void generateForUser_expenseOutpacesRevenue_createsBusinessTask() {
        stubYtdRevenue(new BigDecimal("100000"));
        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);

        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(2);
                    YearMonth month = YearMonth.from(from);
                    if (month.equals(current)) return new BigDecimal("15000");
                    if (month.equals(previous)) return new BigDecimal("10000");
                    return BigDecimal.ZERO;
                });
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(2);
                    YearMonth month = YearMonth.from(from);
                    if (month.equals(current)) return new BigDecimal("11000");
                    if (month.equals(previous)) return new BigDecimal("10000");
                    return BigDecimal.ZERO;
                });

        engine.generateForUser(user);

        verify(taskGenerator).createIfAbsent(
                eq(USER_ID),
                eq("review-expense-growth-" + current),
                eq("Переглянути зростання витрат"),
                contains("зросли"),
                eq(TaskType.BUSINESS),
                eq(TaskPriority.HIGH),
                any(LocalDate.class),
                eq(true),
                eq("AI-рекомендація"),
                anyString(),
                eq(NotificationType.AI_INSIGHT),
                eq(NotificationSeverity.WARNING),
                any(NotificationPreferenceKey.class)
        );
    }

    @Test
    @DisplayName("creates monthly report task in first 10 days of month")
    void generateForUser_earlyInMonth_createsMonthlyReportTask() {
        stubYtdRevenue(new BigDecimal("100000"));

        engine.generateForUser(user);

        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() <= 10) {
            verify(taskGenerator).createIfAbsent(
                    eq(USER_ID),
                    eq("monthly-report-" + YearMonth.now()),
                    eq("Згенерувати місячний звіт"),
                    anyString(),
                    eq(TaskType.REPORTING),
                    eq(TaskPriority.MEDIUM),
                    any(LocalDate.class),
                    eq(false),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull()
            );
        } else {
            verify(taskGenerator, never()).createIfAbsent(
                    eq(USER_ID), eq("monthly-report-" + YearMonth.now()), anyString(), anyString(),
                    any(), any(), any(), anyBoolean(), any(), any(), any(), any(), any());
        }
    }

    @Test
    @DisplayName("skips FOP task when income exceeds all group limits")
    void generateForUser_incomeAboveLimits_noFopTask() {
        stubYtdRevenue(new BigDecimal("10000000"));

        engine.generateForUser(user);

        verify(taskGenerator, never()).createIfAbsent(
                eq(USER_ID),
                contains("fop-limit-review"),
                anyString(),
                anyString(),
                any(),
                any(),
                any(),
                anyBoolean(),
                any(),
                any(),
                any(),
                any(),
                any(NotificationPreferenceKey.class)
        );
    }

    @Test
    @DisplayName("uses CRITICAL priority for FOP usage at or above 95%")
    void generateForUser_fopUsage95_criticalPriority() {
        stubYtdRevenue(new BigDecimal("1600000"));

        engine.generateForUser(user);

        verify(taskGenerator).createIfAbsent(
                eq(USER_ID),
                contains("fop-limit-review"),
                anyString(),
                anyString(),
                eq(TaskType.BUSINESS),
                eq(TaskPriority.CRITICAL),
                any(LocalDate.class),
                eq(true),
                anyString(),
                anyString(),
                eq(NotificationType.FOP_LIMIT),
                eq(NotificationSeverity.CRITICAL),
                any(NotificationPreferenceKey.class)
        );
    }

    @Test
    @DisplayName("handles zero transaction data without throwing")
    void generateForUser_zeroData_noException() {
        stubYtdRevenue(new BigDecimal("100000"));

        assertThatCode(() -> engine.generateForUser(user)).doesNotThrowAnyException();
    }

    private void stubYtdRevenue(BigDecimal amount) {
        stubYtdRevenue(amount, LocalDate.now());
    }

    private void stubYtdRevenue(BigDecimal amount, LocalDate today) {
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), eq(yearStart), eq(today)))
                .thenReturn(amount);
    }
}
