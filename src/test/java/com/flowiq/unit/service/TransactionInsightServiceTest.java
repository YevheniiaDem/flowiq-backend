package com.flowiq.unit.service;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.service.TransactionInsightService;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionInsightService unit tests")
class TransactionInsightServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionInsightService transactionInsightService;

    @Test
    @DisplayName("buildAnalysisContext aggregates revenue and expenses")
    void buildAnalysisContext_success() {
        User user = SecurityTestSupport.testUser(USER_ID, "insights@test.flowiq");
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 6, 30);

        when(transactionRepository.findAll(any(Specification.class)))
                .thenReturn(List.of(
                        sampleTransaction(Transaction.Type.REVENUE, "Services", "5000", LocalDate.of(2026, 3, 1)),
                        sampleTransaction(Transaction.Type.EXPENSE, "Office", "1200", LocalDate.of(2026, 3, 5)),
                        sampleTransaction(Transaction.Type.REVENUE, "Products", "2000", LocalDate.of(2026, 4, 10))
                ));

        TransactionInsightService.TransactionAnalysisContext context =
                transactionInsightService.buildAnalysisContext(user, from, to);

        assertThat(context.getUserId()).isEqualTo(USER_ID);
        assertThat(context.getPeriodFrom()).isEqualTo(from);
        assertThat(context.getPeriodTo()).isEqualTo(to);
        assertThat(context.getTransactionCount()).isEqualTo(3);
        assertThat(context.getTotalRevenue()).isEqualByComparingTo("7000");
        assertThat(context.getTotalExpenses()).isEqualByComparingTo("1200");
        assertThat(context.getNetProfit()).isEqualByComparingTo("5800");
        assertThat(context.getTransactions()).hasSize(3);
    }

    @Test
    @DisplayName("buildAnalysisContext handles empty period")
    void buildAnalysisContext_empty() {
        User user = SecurityTestSupport.testUser(USER_ID, "insights@test.flowiq");
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);

        when(transactionRepository.findAll(any(Specification.class))).thenReturn(List.of());

        TransactionInsightService.TransactionAnalysisContext context =
                transactionInsightService.buildAnalysisContext(user, from, to);

        assertThat(context.getTransactionCount()).isZero();
        assertThat(context.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(context.getTotalExpenses()).isEqualByComparingTo("0");
        assertThat(context.getNetProfit()).isEqualByComparingTo("0");
    }

    private Transaction sampleTransaction(Transaction.Type type, String category, String amount, LocalDate date) {
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setAmount(new BigDecimal(amount));
        transaction.setDescription(category + " tx");
        transaction.setTransactionDate(date);
        return transaction;
    }
}
