package com.flowiq.service;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionSeedService {

    private final TransactionRepository transactionRepository;

    @Transactional
    public void seedIfEmpty(User user) {
        if (transactionRepository.existsByUserId(user.getId())) {
            return;
        }

        YearMonth currentMonth = YearMonth.now();
        YearMonth previousMonth = currentMonth.minusMonths(1);

        List<Transaction> transactions = new ArrayList<>();

        // Current month - revenue ~245,400
        addRevenue(transactions, user, currentMonth, "Online Sales", new BigDecimal("145000"), 1);
        addRevenue(transactions, user, currentMonth, "Subscriptions", new BigDecimal("58000"), 5);
        addRevenue(transactions, user, currentMonth, "Consulting", new BigDecimal("28400"), 10);
        addRevenue(transactions, user, currentMonth, "Partnerships", new BigDecimal("14000"), 15);

        // Current month - expenses ~152,600
        addExpense(transactions, user, currentMonth, "Salaries", new BigDecimal("62000"), 1);
        addExpense(transactions, user, currentMonth, "Marketing", new BigDecimal("45000"), 3);
        addExpense(transactions, user, currentMonth, "Infrastructure", new BigDecimal("18000"), 7);
        addExpense(transactions, user, currentMonth, "Operations", new BigDecimal("15600"), 12);
        addExpense(transactions, user, currentMonth, "Other", new BigDecimal("12000"), 20);

        // Previous month - revenue ~204,300 (for +20.1% growth)
        addRevenue(transactions, user, previousMonth, "Online Sales", new BigDecimal("120000"), 2);
        addRevenue(transactions, user, previousMonth, "Subscriptions", new BigDecimal("48300"), 8);
        addRevenue(transactions, user, previousMonth, "Consulting", new BigDecimal("24000"), 14);
        addRevenue(transactions, user, previousMonth, "Partnerships", new BigDecimal("12000"), 18);

        // Previous month - expenses ~161,000 (for -5.2% change)
        addExpense(transactions, user, previousMonth, "Salaries", new BigDecimal("62000"), 2);
        addExpense(transactions, user, previousMonth, "Marketing", new BigDecimal("36000"), 5);
        addExpense(transactions, user, previousMonth, "Infrastructure", new BigDecimal("21000"), 10);
        addExpense(transactions, user, previousMonth, "Operations", new BigDecimal("16000"), 15);
        addExpense(transactions, user, previousMonth, "Other", new BigDecimal("26000"), 22);

        transactionRepository.saveAll(transactions);
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
