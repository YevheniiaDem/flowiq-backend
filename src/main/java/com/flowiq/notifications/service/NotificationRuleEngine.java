package com.flowiq.notifications.service;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskType;
import com.flowiq.tasks.service.TaskGeneratorService;

@Service
@RequiredArgsConstructor
public class NotificationRuleEngine {

    private static final BigDecimal ESV_MONTHLY = new BigDecimal("1760");
    private static final Map<Integer, BigDecimal> INCOME_LIMITS = Map.of(
            1, new BigDecimal("1672000"),
            2, new BigDecimal("5328000"),
            3, new BigDecimal("7818000")
    );
    private static final Map<Integer, BigDecimal> SINGLE_TAX_RATES = Map.of(
            1, new BigDecimal("0.10"),
            2, new BigDecimal("0.05"),
            3, new BigDecimal("0.03")
    );
    private static final List<Integer> TAX_REMINDER_DAYS = List.of(30, 14, 7, 3, 1);

    private final NotificationGeneratorService notificationGenerator;
    private final TaskGeneratorService taskGenerator;
    private final TransactionRepository transactionRepository;

    public void generateForUser(User user) {
        Long userId = user.getId();
        LocalDate today = LocalDate.now();
        YearMonth current = YearMonth.now();

        generateFopLimitNotifications(userId, today);
        generateTaxDeadlineNotifications(userId, today);
        generateExpenseSpikeNotification(userId, current);
        generateRevenueDropNotification(userId, current);
        generateProfitGrowthNotification(userId, current);
        generateTaxOptimizationNotification(userId, today);
    }

    private void generateFopLimitNotifications(Long userId, LocalDate today) {
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
        BigDecimal annualIncome = sumRange(userId, Transaction.Type.REVENUE, yearStart, today);
        int fopGroup = resolveFopGroup(annualIncome);
        if (fopGroup == 0) {
            return;
        }

        BigDecimal incomeLimit = INCOME_LIMITS.get(fopGroup);
        double usagePercent = annualIncome.multiply(new BigDecimal("100"))
                .divide(incomeLimit, 2, RoundingMode.HALF_UP)
                .doubleValue();

        LocalDateTime expiresAt = LocalDate.of(today.getYear(), 12, 31).atTime(23, 59);

        if (usagePercent >= 95) {
            notificationGenerator.createIfAbsent(
                    userId,
                    "fop-limit-95-" + today.getYear(),
                    "Критичне наближення до ліміту ФОП",
                    "Критичне наближення до ліміту ФОП",
                    NotificationType.FOP_LIMIT,
                    NotificationSeverity.CRITICAL,
                    "/business-guide",
                    expiresAt
            );
        } else if (usagePercent >= 85) {
            notificationGenerator.createIfAbsent(
                    userId,
                    "fop-limit-85-" + today.getYear(),
                    "Ліміт ФОП",
                    "До ліміту ФОП залишилось менше 15%",
                    NotificationType.FOP_LIMIT,
                    NotificationSeverity.WARNING,
                    "/business-guide",
                    expiresAt
            );
        } else if (usagePercent >= 70) {
            notificationGenerator.createIfAbsent(
                    userId,
                    "fop-limit-70-" + today.getYear(),
                    "Ліміт ФОП",
                    String.format(Locale.forLanguageTag("uk-UA"), "Використано %.0f%% річного ліміту ФОП", usagePercent),
                    NotificationType.FOP_LIMIT,
                    NotificationSeverity.WARNING,
                    "/business-guide",
                    expiresAt
            );
        }
    }

    private void generateTaxDeadlineNotifications(Long userId, LocalDate today) {
        int year = today.getYear();
        List<LocalDate> deadlines = List.of(
                LocalDate.of(year, 5, 10),
                LocalDate.of(year, 8, 9),
                LocalDate.of(year, 11, 9),
                LocalDate.of(year + 1, 2, 9)
        );

        for (LocalDate deadline : deadlines) {
            if (deadline.isBefore(today)) {
                continue;
            }
            int daysUntil = (int) java.time.temporal.ChronoUnit.DAYS.between(today, deadline);
            if (!TAX_REMINDER_DAYS.contains(daysUntil)) {
                continue;
            }

            NotificationSeverity severity = resolveTaxSeverity(daysUntil);
            String message = buildTaxMessage(daysUntil);
            String key = "tax-deadline-" + deadline + "-" + daysUntil;

            notificationGenerator.createIfAbsent(
                    userId,
                    key,
                    "Нагадування про податок",
                    message,
                    NotificationType.TAX,
                    severity,
                    "/ai-accountant",
                    deadline.atTime(23, 59)
            );
            taskGenerator.createFromNotification(
                    userId,
                    key,
                    "Нагадування про податок",
                    message,
                    TaskType.TAX,
                    severity == NotificationSeverity.CRITICAL
                            ? TaskPriority.CRITICAL
                            : TaskPriority.HIGH,
                    deadline
            );
        }
    }

    private void generateExpenseSpikeNotification(Long userId, YearMonth current) {
        BigDecimal currentExpenses = sum(userId, Transaction.Type.EXPENSE, current);
        BigDecimal avgPreviousThree = averageExpensesPreviousMonths(userId, current, 3);

        if (avgPreviousThree.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        double increasePercent = currentExpenses.subtract(avgPreviousThree)
                .divide(avgPreviousThree, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();

        if (increasePercent > 20) {
            String key = "expense-spike-" + current;
            String msg = String.format(Locale.forLanguageTag("uk-UA"),
                    "Витрати виросли на %.0f%% порівняно із середнім значенням", increasePercent);
            notificationGenerator.createIfAbsent(
                    userId,
                    key,
                    "Зростання витрат",
                    msg,
                    NotificationType.FINANCIAL,
                    NotificationSeverity.WARNING,
                    "/analytics",
                    current.atEndOfMonth().atTime(23, 59)
            );
            taskGenerator.createFromNotification(
                    userId, key, "Переглянути зростання витрат", msg,
                    TaskType.BUSINESS, TaskPriority.HIGH, current.atEndOfMonth()
            );
        }
    }

    private void generateRevenueDropNotification(Long userId, YearMonth current) {
        BigDecimal currentRevenue = sum(userId, Transaction.Type.REVENUE, current);
        BigDecimal avgPreviousThree = averageRevenuePreviousMonths(userId, current, 3);

        if (avgPreviousThree.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        double changePercent = currentRevenue.subtract(avgPreviousThree)
                .divide(avgPreviousThree, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();

        if (changePercent < -20) {
            double dropPercent = Math.abs(changePercent);
            String key = "revenue-drop-" + current;
            notificationGenerator.createIfAbsent(
                    userId,
                    key,
                    "Падіння доходу",
                    String.format(Locale.forLanguageTag("uk-UA"), "Дохід впав на %.0f%%", dropPercent),
                    NotificationType.FINANCIAL,
                    NotificationSeverity.WARNING,
                    "/analytics",
                    current.atEndOfMonth().atTime(23, 59)
            );
        }
    }

    private void generateProfitGrowthNotification(Long userId, YearMonth current) {
        List<BigDecimal> lastThreeProfit = new java.util.ArrayList<>();
        for (int i = 2; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            BigDecimal revenue = sum(userId, Transaction.Type.REVENUE, month);
            BigDecimal expenses = sum(userId, Transaction.Type.EXPENSE, month);
            lastThreeProfit.add(revenue.subtract(expenses));
        }

        boolean growing = lastThreeProfit.size() == 3
                && lastThreeProfit.get(0).compareTo(lastThreeProfit.get(1)) < 0
                && lastThreeProfit.get(1).compareTo(lastThreeProfit.get(2)) < 0;

        if (growing) {
            String key = "profit-growth-" + current;
            notificationGenerator.createIfAbsent(
                    userId,
                    key,
                    "Зростання прибутку",
                    "Ваш прибуток стабільно зростає вже 3 місяці",
                    NotificationType.FINANCIAL,
                    NotificationSeverity.SUCCESS,
                    "/analytics",
                    current.atEndOfMonth().atTime(23, 59)
            );
        }
    }

    private void generateTaxOptimizationNotification(Long userId, LocalDate today) {
        LocalDate yearStart = LocalDate.of(today.getYear(), 1, 1);
        BigDecimal annualIncome = sumRange(userId, Transaction.Type.REVENUE, yearStart, today);
        int optimalGroup = resolveFopGroup(annualIncome);

        if (optimalGroup <= 1) {
            return;
        }

        int lowerGroup = optimalGroup - 1;
        if (annualIncome.compareTo(INCOME_LIMITS.get(lowerGroup)) <= 0) {
            return;
        }

        BigDecimal taxIfLower = estimateTaxLoad(annualIncome, lowerGroup);
        BigDecimal taxOptimal = estimateTaxLoad(annualIncome, optimalGroup);
        BigDecimal savings = taxIfLower.subtract(taxOptimal);

        if (savings.compareTo(new BigDecimal("1000")) > 0) {
            String key = "tax-optimization-" + today.getYear() + "-g" + optimalGroup;
            notificationGenerator.createIfAbsent(
                    userId,
                    key,
                    "Оптимізація податків",
                    String.format(Locale.forLanguageTag("uk-UA"),
                            "Можна зекономити до %s на рік, перейшовши на ФОП групу %d",
                            formatAmount(savings), optimalGroup),
                    NotificationType.AI_INSIGHT,
                    NotificationSeverity.INFO,
                    "/business-guide",
                    LocalDate.of(today.getYear(), 12, 31).atTime(23, 59)
            );
        }
    }

    private NotificationSeverity resolveTaxSeverity(int daysUntil) {
        if (daysUntil <= 1) {
            return NotificationSeverity.CRITICAL;
        }
        if (daysUntil <= 7) {
            return NotificationSeverity.WARNING;
        }
        return NotificationSeverity.INFO;
    }

    private String buildTaxMessage(int daysUntil) {
        if (daysUntil == 1) {
            return "Завтра останній день сплати ЄСВ";
        }
        return String.format(Locale.forLanguageTag("uk-UA"), "Через %d днів потрібно сплатити податок", daysUntil);
    }

    private int resolveFopGroup(BigDecimal annualIncome) {
        if (annualIncome.compareTo(INCOME_LIMITS.get(1)) <= 0) {
            return 1;
        }
        if (annualIncome.compareTo(INCOME_LIMITS.get(2)) <= 0) {
            return 2;
        }
        if (annualIncome.compareTo(INCOME_LIMITS.get(3)) <= 0) {
            return 3;
        }
        return 0;
    }

    private BigDecimal estimateTaxLoad(BigDecimal income, int fopGroup) {
        if (income.compareTo(BigDecimal.ZERO) <= 0 || fopGroup == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal singleTax = income.multiply(SINGLE_TAX_RATES.get(fopGroup));
        BigDecimal esv = ESV_MONTHLY.multiply(new BigDecimal(LocalDate.now().getMonthValue()));
        return singleTax.add(esv).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageExpensesPreviousMonths(Long userId, YearMonth current, int months) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 1; i <= months; i++) {
            total = total.add(sum(userId, Transaction.Type.EXPENSE, current.minusMonths(i)));
        }
        return total.divide(new BigDecimal(months), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal averageRevenuePreviousMonths(Long userId, YearMonth current, int months) {
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 1; i <= months; i++) {
            total = total.add(sum(userId, Transaction.Type.REVENUE, current.minusMonths(i)));
        }
        return total.divide(new BigDecimal(months), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(Long userId, Transaction.Type type, YearMonth month) {
        return transactionRepository.sumByUserAndTypeAndDateRange(
                userId, type, month.atDay(1), month.atEndOfMonth());
    }

    private BigDecimal sumRange(Long userId, Transaction.Type type, LocalDate start, LocalDate end) {
        return transactionRepository.sumByUserAndTypeAndDateRange(userId, type, start, end);
    }

    private String formatAmount(BigDecimal amount) {
        return String.format(Locale.forLanguageTag("uk-UA"), "%,.0f ₴", amount);
    }
}
