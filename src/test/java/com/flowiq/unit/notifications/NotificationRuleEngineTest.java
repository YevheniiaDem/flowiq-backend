package com.flowiq.unit.notifications;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.service.NotificationGeneratorService;
import com.flowiq.notifications.service.NotificationRuleEngine;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.tasks.service.TaskGeneratorService;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationRuleEngine unit tests")
class NotificationRuleEngineTest {

    private static final Long USER_ID = 42L;

    @Mock
    private NotificationGeneratorService notificationGenerator;
    @Mock
    private TaskGeneratorService taskGenerator;
    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private NotificationRuleEngine engine;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(USER_ID);
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                anyLong(), any(Transaction.Type.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
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

    @Test
    @DisplayName("creates tax deadline reminder exactly 30 days before deadline")
    void generateForUser_taxReminder30Days_createsNotification() {
        LocalDate today = LocalDate.of(2026, 4, 10);
        try (MockedStatic<LocalDate> localDate = org.mockito.Mockito.mockStatic(LocalDate.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
            localDate.when(LocalDate::now).thenReturn(today);
            stubYtdRevenue(new BigDecimal("100000"), today);

            engine.generateForUser(user);

            verify(notificationGenerator).createIfAbsent(
                    eq(USER_ID),
                    eq("tax-deadline-2026-05-10-30"),
                    eq("Нагадування про податок"),
                    contains("30"),
                    eq(NotificationType.TAX),
                    eq(NotificationSeverity.INFO),
                    eq("/ai-accountant"),
                    any()
            );
            verify(taskGenerator).createFromNotification(
                    eq(USER_ID),
                    eq("tax-deadline-2026-05-10-30"),
                    anyString(),
                    anyString(),
                    eq(com.flowiq.tasks.entity.TaskType.TAX),
                    any(),
                    any()
            );
        }
    }

    @Test
    @DisplayName("creates FOP 70% warning notification")
    void generateForUser_fopLimit70_warningNotification() {
        stubYtdRevenue(new BigDecimal("1200000"));

        engine.generateForUser(user);

        verify(notificationGenerator).createIfAbsent(
                eq(USER_ID),
                contains("fop-limit-70"),
                anyString(),
                contains("Використано"),
                eq(NotificationType.FOP_LIMIT),
                eq(NotificationSeverity.WARNING),
                eq("/business-guide"),
                any()
        );
    }

    @Test
    @DisplayName("creates critical FOP notification when usage exceeds 95%")
    void generateForUser_fopLimit95_criticalNotification() {
        stubYtdRevenue(new BigDecimal("1600000"));

        engine.generateForUser(user);

        verify(notificationGenerator).createIfAbsent(
                eq(USER_ID),
                contains("fop-limit-95"),
                anyString(),
                anyString(),
                eq(NotificationType.FOP_LIMIT),
                eq(NotificationSeverity.CRITICAL),
                eq("/business-guide"),
                any()
        );
    }

    @Test
    @DisplayName("creates warning FOP notification when usage between 85% and 95%")
    void generateForUser_fopLimit85_warningNotification() {
        stubYtdRevenue(new BigDecimal("1450000"));

        engine.generateForUser(user);

        verify(notificationGenerator).createIfAbsent(
                eq(USER_ID),
                contains("fop-limit-85"),
                anyString(),
                anyString(),
                eq(NotificationType.FOP_LIMIT),
                eq(NotificationSeverity.WARNING),
                eq("/business-guide"),
                any()
        );
    }

    @Test
    @DisplayName("skips FOP notifications when income exceeds all group limits")
    void generateForUser_incomeAboveAllLimits_noFopNotification() {
        stubYtdRevenue(new BigDecimal("9000000"));

        engine.generateForUser(user);

        verify(notificationGenerator, never()).createIfAbsent(
                eq(USER_ID),
                contains("fop-limit"),
                anyString(),
                anyString(),
                eq(NotificationType.FOP_LIMIT),
                any(),
                anyString(),
                any()
        );
    }

    @Test
    @DisplayName("creates expense spike notification when increase exceeds 20%")
    void generateForUser_expenseSpike_createsNotification() {
        stubYtdRevenue(new BigDecimal("100000"));
        YearMonth current = YearMonth.now();

        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(2);
                    YearMonth month = YearMonth.from(from);
                    if (month.equals(current)) {
                        return new BigDecimal("15000");
                    }
                    return new BigDecimal("10000");
                });
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("20000"));

        engine.generateForUser(user);

        verify(notificationGenerator).createIfAbsent(
                eq(USER_ID),
                eq("expense-spike-" + current),
                eq("Зростання витрат"),
                contains("виросли"),
                eq(NotificationType.FINANCIAL),
                eq(NotificationSeverity.WARNING),
                eq("/analytics"),
                any()
        );
        verify(taskGenerator).createFromNotification(
                eq(USER_ID), eq("expense-spike-" + current), anyString(), anyString(),
                any(), any(), any()
        );
    }

    @Test
    @DisplayName("creates revenue drop notification when revenue falls more than 20%")
    void generateForUser_revenueDrop_createsNotification() {
        stubYtdRevenue(new BigDecimal("100000"));
        YearMonth current = YearMonth.now();

        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(2);
                    YearMonth month = YearMonth.from(from);
                    if (month.equals(current)) {
                        return new BigDecimal("5000");
                    }
                    return new BigDecimal("10000");
                });
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);

        engine.generateForUser(user);

        verify(notificationGenerator).createIfAbsent(
                eq(USER_ID),
                eq("revenue-drop-" + current),
                eq("Падіння доходу"),
                contains("впав"),
                eq(NotificationType.FINANCIAL),
                eq(NotificationSeverity.WARNING),
                eq("/analytics"),
                any()
        );
    }

    @Test
    @DisplayName("creates profit growth notification for three consecutive growing months")
    void generateForUser_profitGrowth_successNotification() {
        stubYtdRevenue(new BigDecimal("100000"));
        YearMonth current = YearMonth.now();

        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.REVENUE), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate from = invocation.getArgument(2);
                    YearMonth month = YearMonth.from(from);
                    if (month.equals(current.minusMonths(2))) return new BigDecimal("10000");
                    if (month.equals(current.minusMonths(1))) return new BigDecimal("12000");
                    if (month.equals(current)) return new BigDecimal("15000");
                    return BigDecimal.ZERO;
                });
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                eq(USER_ID), eq(Transaction.Type.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("5000"));

        engine.generateForUser(user);

        verify(notificationGenerator).createIfAbsent(
                eq(USER_ID),
                eq("profit-growth-" + current),
                eq("Зростання прибутку"),
                anyString(),
                eq(NotificationType.FINANCIAL),
                eq(NotificationSeverity.SUCCESS),
                eq("/analytics"),
                any()
        );
    }

    @Test
    @DisplayName("skips expense spike when previous average is zero")
    void generateForUser_zeroPreviousExpenses_noSpikeNotification() {
        stubYtdRevenue(new BigDecimal("100000"));

        engine.generateForUser(user);

        verify(notificationGenerator, never()).createIfAbsent(
                eq(USER_ID),
                contains("expense-spike"),
                anyString(),
                anyString(),
                eq(NotificationType.FINANCIAL),
                any(),
                anyString(),
                any()
        );
    }

    @Test
    @DisplayName("handles zero annual income without FOP limit notifications")
    void generateForUser_zeroIncome_noFopNotifications() {
        stubYtdRevenue(BigDecimal.ZERO);

        engine.generateForUser(user);

        verify(notificationGenerator, never()).createIfAbsent(
                eq(USER_ID),
                contains("fop-limit"),
                anyString(),
                anyString(),
                eq(NotificationType.FOP_LIMIT),
                any(),
                anyString(),
                any()
        );
    }
}
