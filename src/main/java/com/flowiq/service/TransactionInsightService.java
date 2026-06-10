package com.flowiq.service;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.repository.TransactionRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Preparation layer for future AI-powered transaction analysis.
 * Provides structured aggregates that an AI provider can consume without
 * exposing raw persistence details to the frontend.
 */
@Service
@RequiredArgsConstructor
public class TransactionInsightService {

    private final TransactionRepository transactionRepository;

    public TransactionAnalysisContext buildAnalysisContext(User user, LocalDate from, LocalDate to) {
        List<Transaction> transactions = transactionRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("user").get("id"), user.getId()),
                cb.greaterThanOrEqualTo(root.get("transactionDate"), from),
                cb.lessThanOrEqualTo(root.get("transactionDate"), to)
        ));

        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            if (transaction.getType() == Transaction.Type.REVENUE) {
                revenue = revenue.add(transaction.getAmount());
            } else {
                expenses = expenses.add(transaction.getAmount());
            }
        }

        return TransactionAnalysisContext.builder()
                .userId(user.getId())
                .periodFrom(from)
                .periodTo(to)
                .transactionCount(transactions.size())
                .totalRevenue(revenue)
                .totalExpenses(expenses)
                .netProfit(revenue.subtract(expenses))
                .transactions(transactions.stream().map(this::toSnapshot).toList())
                .build();
    }

    private TransactionSnapshot toSnapshot(Transaction transaction) {
        return TransactionSnapshot.builder()
                .type(transaction.getType().name())
                .amount(transaction.getAmount())
                .category(transaction.getCategory())
                .description(transaction.getDescription())
                .transactionDate(transaction.getTransactionDate())
                .build();
    }

    @Data
    @Builder
    public static class TransactionAnalysisContext {
        private Long userId;
        private LocalDate periodFrom;
        private LocalDate periodTo;
        private long transactionCount;
        private BigDecimal totalRevenue;
        private BigDecimal totalExpenses;
        private BigDecimal netProfit;
        private List<TransactionSnapshot> transactions;
    }

    @Data
    @Builder
    public static class TransactionSnapshot {
        private String type;
        private BigDecimal amount;
        private String category;
        private String description;
        private LocalDate transactionDate;
    }
}
