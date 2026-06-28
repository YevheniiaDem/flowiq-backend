package com.flowiq.integration.repository;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.integration.support.AbstractPostgresIntegrationTest;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionRepository integration tests")
class TransactionRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("repo-tx-" + System.nanoTime() + "@test.flowiq");
        user.setPassword("encoded");
        user.setName("Repo User");
        user.setRole(User.Role.USER);
        user.setActive(true);
        user = userRepository.save(user);
    }

    @Test
    @DisplayName("CRUD: save and find by id and user")
    @Transactional
    void crud_saveAndFind() {
        Transaction saved = transactionRepository.save(sampleTransaction(
                Transaction.Type.EXPENSE, new BigDecimal("500"), "Office", LocalDate.of(2026, 5, 1)));

        assertThat(transactionRepository.findByIdAndUserId(saved.getId(), user.getId())).isPresent();
    }

    @Test
    @DisplayName("JPQL: sumByUserAndTypeAndDateRange aggregates amounts")
    @Transactional
    void jpql_sumByTypeAndDateRange() {
        transactionRepository.save(sampleTransaction(
                Transaction.Type.REVENUE, new BigDecimal("1000"), "Services", LocalDate.of(2026, 6, 1)));
        transactionRepository.save(sampleTransaction(
                Transaction.Type.REVENUE, new BigDecimal("2000"), "Services", LocalDate.of(2026, 6, 15)));
        transactionRepository.save(sampleTransaction(
                Transaction.Type.REVENUE, new BigDecimal("500"), "Services", LocalDate.of(2026, 7, 1)));

        BigDecimal sum = transactionRepository.sumByUserAndTypeAndDateRange(
                user.getId(),
                Transaction.Type.REVENUE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30));

        assertThat(sum).isEqualByComparingTo(new BigDecimal("3000"));
    }

    @Test
    @DisplayName("JPQL: existsDuplicate detects matching transaction")
    @Transactional
    void jpql_existsDuplicate() {
        LocalDate date = LocalDate.of(2026, 6, 10);
        transactionRepository.save(sampleTransaction(
                Transaction.Type.EXPENSE, new BigDecimal("777"), "Office", date, "Duplicate check"));

        boolean exists = transactionRepository.existsDuplicate(
                user.getId(), date, new BigDecimal("777"), Transaction.Type.EXPENSE, "Duplicate check");

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("JPQL: sumExpensesByCategory groups and sorts categories")
    @Transactional
    void jpql_sumExpensesByCategory() {
        transactionRepository.save(sampleTransaction(
                Transaction.Type.EXPENSE, new BigDecimal("300"), "Office", LocalDate.of(2026, 6, 1)));
        transactionRepository.save(sampleTransaction(
                Transaction.Type.EXPENSE, new BigDecimal("700"), "Marketing", LocalDate.of(2026, 6, 2)));

        List<TransactionRepository.CategorySumProjection> sums = transactionRepository.sumExpensesByCategory(
                user.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));

        assertThat(sums).hasSize(2);
        assertThat(sums.get(0).getCategory()).isEqualTo("Marketing");
        assertThat(sums.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("700"));
    }

    @Test
    @DisplayName("Filtering: findAll with specification by user")
    @Transactional
    void filtering_byUser() {
        transactionRepository.save(sampleTransaction(
                Transaction.Type.EXPENSE, new BigDecimal("100"), "Office", LocalDate.of(2026, 6, 1)));

        long count = transactionRepository.findAll((root, query, cb) ->
                cb.equal(root.get("user").get("id"), user.getId())).size();

        assertThat(count).isEqualTo(1);
    }

    private Transaction sampleTransaction(Transaction.Type type, BigDecimal amount, String category, LocalDate date) {
        return sampleTransaction(type, amount, category, date, null);
    }

    private Transaction sampleTransaction(
            Transaction.Type type, BigDecimal amount, String category, LocalDate date, String description) {
        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setCategory(category);
        transaction.setDescription(description);
        transaction.setTransactionDate(date);
        transaction.setAutoCategorized(false);
        return transaction;
    }
}
