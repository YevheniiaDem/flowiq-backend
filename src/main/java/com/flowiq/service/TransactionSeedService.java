package com.flowiq.service;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionSeedService {

    private static final BigDecimal[] MONTHLY_REVENUE_TARGETS = {
            new BigDecimal("150000"),
            new BigDecimal("165000"),
            new BigDecimal("178000"),
            new BigDecimal("190000"),
            new BigDecimal("204300"),
            new BigDecimal("245400"),
    };

    private final TransactionRepository transactionRepository;

    @Transactional
    public void seedIfEmpty(User user) {
        if (transactionRepository.existsByUserId(user.getId())) {
            ensureSixMonthHistory(user);
            return;
        }

        YearMonth current = YearMonth.now();
        List<Transaction> transactions = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            int targetIndex = 5 - i;
            addMonthTransactions(transactions, user, month, MONTHLY_REVENUE_TARGETS[targetIndex]);
        }

        transactionRepository.saveAll(transactions);
    }

    @Transactional
    public void ensureSixMonthHistory(User user) {
        YearMonth current = YearMonth.now();
        List<Transaction> toSave = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            int targetIndex = 5 - i;
            BigDecimal revenue = transactionRepository.sumByUserAndTypeAndDateRange(
                    user.getId(), Transaction.Type.REVENUE,
                    month.atDay(1), month.atEndOfMonth());

            if (revenue.compareTo(BigDecimal.ZERO) == 0) {
                addMonthTransactions(toSave, user, month, MONTHLY_REVENUE_TARGETS[targetIndex]);
            }
        }

        if (!toSave.isEmpty()) {
            transactionRepository.saveAll(toSave);
        }
    }

    private void addMonthTransactions(List<Transaction> list, User user, YearMonth month,
                                      BigDecimal revenueTarget) {
        BigDecimal expenseTarget = revenueTarget.multiply(new BigDecimal("0.62"))
                .setScale(0, RoundingMode.HALF_UP);

        addRevenue(list, user, month, "Online Sales", scale(revenueTarget, "0.59"), 1);
        addRevenue(list, user, month, "Subscriptions", scale(revenueTarget, "0.24"), 5);
        addRevenue(list, user, month, "Consulting", scale(revenueTarget, "0.12"), 10);
        addRevenue(list, user, month, "Partnerships", scale(revenueTarget, "0.05"), 15);

        addExpense(list, user, month, "Salaries", scale(expenseTarget, "0.41"), 1);
        addExpense(list, user, month, "Marketing", scale(expenseTarget, "0.29"), 3);
        addExpense(list, user, month, "Infrastructure", scale(expenseTarget, "0.12"), 7);
        addExpense(list, user, month, "Operations", scale(expenseTarget, "0.10"), 12);
        addExpense(list, user, month, "Other", scale(expenseTarget, "0.08"), 20);
    }

    private BigDecimal scale(BigDecimal base, String ratio) {
        return base.multiply(new BigDecimal(ratio)).setScale(0, RoundingMode.HALF_UP);
    }

    private void addRevenue(List<Transaction> list, User user, YearMonth month,
                            String category, BigDecimal amount, int day) {
        list.add(buildTransaction(user, Transaction.Type.REVENUE, category, amount,
                month.atDay(Math.min(day, month.lengthOfMonth()))));
    }

    private void addExpense(List<Transaction> list, User user, YearMonth month,
                            String category, BigDecimal amount, int day) {
        list.add(buildTransaction(user, Transaction.Type.EXPENSE, category, amount,
                month.atDay(Math.min(day, month.lengthOfMonth()))));
    }

    private Transaction buildTransaction(User user, Transaction.Type type, String category,
                                         BigDecimal amount, LocalDate date) {
        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setType(type);
        tx.setCategory(category);
        tx.setAmount(amount);
        tx.setDescription(category + " transaction");
        tx.setTransactionDate(date);
        return tx;
    }
}
