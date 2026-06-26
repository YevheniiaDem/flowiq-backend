package com.flowiq.tasks.service;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.preferences.NotificationPreferenceKey;
import com.flowiq.notifications.preferences.NotificationPreferenceKeys;
import com.flowiq.profile.service.FopProfileService;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskRuleEngine {

    private static final Map<Integer, BigDecimal> INCOME_LIMITS = Map.of(
            1, new BigDecimal("1672000"),
            2, new BigDecimal("5328000"),
            3, new BigDecimal("7818000")
    );

    private final TaskGeneratorService taskGenerator;
    private final TransactionRepository transactionRepository;
    private final FopProfileService fopProfileService;

    public void generateForUser(User user) {
        Long userId = user.getId();
        LocalDate today = LocalDate.now();

        generateTaxPaymentTasks(userId, today);
        generateEsvPaymentTasks(userId, today);
        generateQuarterDeclarationTasks(userId, today);
        generateAnnualDeclarationTask(userId, today);
        generateFopLimitReviewTask(userId, today);
        generateBusinessInsightTasks(userId, today);
    }

    private void generateTaxPaymentTasks(Long userId, LocalDate today) {
        for (LocalDate deadline : taxDeadlines(today.getYear())) {
            if (deadline.isBefore(today)) {
                continue;
            }
            int daysUntil = (int) ChronoUnit.DAYS.between(today, deadline);
            if (daysUntil > 30) {
                continue;
            }

            TaskPriority priority = daysUntil <= 3
                    ? TaskPriority.CRITICAL
                    : daysUntil <= 7 ? TaskPriority.HIGH : TaskPriority.MEDIUM;

            taskGenerator.createIfAbsent(
                    userId,
                    "tax-payment-" + deadline,
                    "Сплатити єдиний податок",
                    String.format(Locale.forLanguageTag("uk-UA"),
                            "Дедлайн сплати єдиного податку — %s (через %d дн.)",
                            deadline, daysUntil),
                    TaskType.TAX,
                    priority,
                    deadline,
                    daysUntil <= 7,
                    "Нагадування про податок",
                    String.format("До сплати єдиного податку залишилось %d дн.", daysUntil),
                    NotificationType.TAX,
                    daysUntil <= 3 ? NotificationSeverity.CRITICAL : NotificationSeverity.WARNING,
                    NotificationPreferenceKeys.taskReminderKey(today, deadline)
            );
        }
    }

    private void generateEsvPaymentTasks(Long userId, LocalDate today) {
        for (LocalDate deadline : taxDeadlines(today.getYear())) {
            if (deadline.isBefore(today)) {
                continue;
            }
            int daysUntil = (int) ChronoUnit.DAYS.between(today, deadline);
            if (daysUntil > 30) {
                continue;
            }

            TaskPriority priority = daysUntil <= 3
                    ? TaskPriority.CRITICAL
                    : daysUntil <= 7 ? TaskPriority.HIGH : TaskPriority.MEDIUM;

            taskGenerator.createIfAbsent(
                    userId,
                    "esv-payment-" + deadline,
                    "Сплатити ЄСВ",
                    String.format(Locale.forLanguageTag("uk-UA"),
                            "Дедлайн сплати ЄСВ — %s (через %d дн.)", deadline, daysUntil),
                    TaskType.TAX,
                    priority,
                    deadline,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    private void generateQuarterDeclarationTasks(Long userId, LocalDate today) {
        for (LocalDate deadline : taxDeadlines(today.getYear())) {
            if (deadline.isBefore(today)) {
                continue;
            }
            int daysUntil = (int) ChronoUnit.DAYS.between(today, deadline);
            if (daysUntil > 14) {
                continue;
            }

            taskGenerator.createIfAbsent(
                    userId,
                    "quarter-declaration-" + deadline,
                    "Подати квартальну декларацію",
                    String.format("Підготуйте та подайте квартальну декларацію до %s", deadline),
                    TaskType.REPORTING,
                    daysUntil <= 7 ? TaskPriority.HIGH : TaskPriority.MEDIUM,
                    deadline.minusDays(2),
                    daysUntil <= 7,
                    "Дедлайн декларації",
                    String.format("Квартальна декларація до %s", deadline),
                    NotificationType.TAX,
                    NotificationSeverity.WARNING,
                    NotificationPreferenceKeys.taskReminderKey(today, deadline)
            );
        }
    }

    private void generateAnnualDeclarationTask(Long userId, LocalDate today) {
        LocalDate annualDeadline = LocalDate.of(today.getYear() + 1, 2, 9);
        if (today.getMonthValue() < 10) {
            return;
        }
        if (annualDeadline.isBefore(today)) {
            return;
        }

        int daysUntil = (int) ChronoUnit.DAYS.between(today, annualDeadline);
        if (daysUntil > 60) {
            return;
        }

        taskGenerator.createIfAbsent(
                userId,
                "annual-declaration-" + annualDeadline.getYear(),
                "Підготувати річну декларацію",
                "Підготуйте річну податкову декларацію ФОП до " + annualDeadline,
                TaskType.REPORTING,
                daysUntil <= 14 ? TaskPriority.HIGH : TaskPriority.MEDIUM,
                annualDeadline.minusDays(7),
                daysUntil <= 30,
                "Річна декларація",
                "Час готувати річну декларацію ФОП",
                NotificationType.TAX,
                NotificationSeverity.INFO,
                NotificationPreferenceKeys.taskReminderKey(today, annualDeadline)
        );
    }

    private void generateFopLimitReviewTask(Long userId, LocalDate today) {
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
        BigDecimal annualIncome = sumRange(userId, Transaction.Type.REVENUE, yearStart, today);
        int fopGroup = fopProfileService.resolveEffectiveFopGroup(userId, annualIncome);
        if (fopGroup == 0) {
            return;
        }

        BigDecimal incomeLimit = INCOME_LIMITS.get(fopGroup);
        double usagePercent = annualIncome.multiply(new BigDecimal("100"))
                .divide(incomeLimit, 2, RoundingMode.HALF_UP)
                .doubleValue();

        if (usagePercent < 70) {
            return;
        }

        TaskPriority priority = usagePercent >= 95
                ? TaskPriority.CRITICAL
                : usagePercent >= 85 ? TaskPriority.HIGH : TaskPriority.MEDIUM;

        taskGenerator.createIfAbsent(
                userId,
                "fop-limit-review-" + today.getYear(),
                "Переглянути ліміт ФОП",
                String.format(Locale.forLanguageTag("uk-UA"),
                        "Використано %.0f%% річного ліміту. Перевірте податкову групу та план доходу.",
                        usagePercent),
                TaskType.BUSINESS,
                priority,
                today.plusDays(7),
                usagePercent >= 85,
                "Ліміт ФОП",
                String.format("Використано %.0f%% ліміту ФОП — потрібен перегляд", usagePercent),
                NotificationType.FOP_LIMIT,
                usagePercent >= 95 ? NotificationSeverity.CRITICAL : NotificationSeverity.WARNING,
                NotificationPreferenceKey.FINANCIAL_TAX_WARNING
        );
    }

    private void generateBusinessInsightTasks(Long userId, LocalDate today) {
        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);

        BigDecimal currentExpenses = sum(userId, Transaction.Type.EXPENSE, current);
        BigDecimal previousExpenses = sum(userId, Transaction.Type.EXPENSE, previous);
        BigDecimal currentRevenue = sum(userId, Transaction.Type.REVENUE, current);
        BigDecimal previousRevenue = sum(userId, Transaction.Type.REVENUE, previous);

        double expenseGrowth = percentChange(currentExpenses, previousExpenses);
        double revenueGrowth = percentChange(currentRevenue, previousRevenue);

        if (expenseGrowth > revenueGrowth + 10 && expenseGrowth > 10) {
            taskGenerator.createIfAbsent(
                    userId,
                    "review-expense-growth-" + current,
                    "Переглянути зростання витрат",
                    String.format(Locale.forLanguageTag("uk-UA"),
                            "Витрати зросли на %.1f%% — перевірте бюджет та ROI маркетингу.",
                            expenseGrowth),
                    TaskType.BUSINESS,
                    TaskPriority.HIGH,
                    today.plusDays(5),
                    true,
                    "AI-рекомендація",
                    "Витрати зростають швидше за дохід",
                    NotificationType.AI_INSIGHT,
                    NotificationSeverity.WARNING,
                    NotificationPreferenceKey.AI_WARNINGS
            );
        }

        if (today.getDayOfMonth() <= 10) {
            taskGenerator.createIfAbsent(
                    userId,
                    "monthly-report-" + current,
                    "Згенерувати місячний звіт",
                    "Підготуйте місячний фінансовий звіт за попередній період",
                    TaskType.REPORTING,
                    TaskPriority.MEDIUM,
                    today.plusDays(5),
                    false,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        LocalDate nextTaxDeadline = resolveNextTaxDeadline(today);
        int daysUntilTax = (int) ChronoUnit.DAYS.between(today, nextTaxDeadline);
        if (daysUntilTax <= 14 && daysUntilTax > 0) {
            taskGenerator.createIfAbsent(
                    userId,
                    "prepare-tax-payment-" + nextTaxDeadline,
                    "Підготувати податковий платіж",
                    String.format("Підготуйте кошти для сплати податків до %s", nextTaxDeadline),
                    TaskType.TAX,
                    daysUntilTax <= 7 ? TaskPriority.HIGH : TaskPriority.MEDIUM,
                    nextTaxDeadline.minusDays(3),
                    daysUntilTax <= 7,
                    "Підготовка до податку",
                    String.format("До сплати податку %d дн.", daysUntilTax),
                    NotificationType.TAX,
                    NotificationSeverity.WARNING,
                    NotificationPreferenceKeys.taskReminderKey(today, nextTaxDeadline)
            );
        }
    }

    private List<LocalDate> taxDeadlines(int year) {
        return List.of(
                LocalDate.of(year, 5, 10),
                LocalDate.of(year, 8, 9),
                LocalDate.of(year, 11, 9),
                LocalDate.of(year + 1, 2, 9)
        );
    }

    private LocalDate resolveNextTaxDeadline(LocalDate today) {
        for (LocalDate deadline : taxDeadlines(today.getYear())) {
            if (!deadline.isBefore(today)) {
                return deadline;
            }
        }
        return LocalDate.of(today.getYear() + 1, 5, 10);
    }

    private BigDecimal sum(Long userId, Transaction.Type type, YearMonth month) {
        return transactionRepository.sumByUserAndTypeAndDateRange(
                userId, type, month.atDay(1), month.atEndOfMonth());
    }

    private BigDecimal sumRange(Long userId, Transaction.Type type, LocalDate start, LocalDate end) {
        return transactionRepository.sumByUserAndTypeAndDateRange(userId, type, start, end);
    }

    private double percentChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }
}
