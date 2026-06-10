package com.flowiq.repository;

import com.flowiq.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    boolean existsByUserId(Long userId);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t
        WHERE t.user.id = :userId AND t.type = :type
        AND t.transactionDate BETWEEN :start AND :end
        """)
    BigDecimal sumByUserAndTypeAndDateRange(
            @Param("userId") Long userId,
            @Param("type") Transaction.Type type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        SELECT t.category AS category, COALESCE(SUM(t.amount), 0) AS amount
        FROM Transaction t
        WHERE t.user.id = :userId AND t.type = com.flowiq.entity.Transaction$Type.EXPENSE
        AND t.transactionDate BETWEEN :start AND :end
        GROUP BY t.category
        ORDER BY SUM(t.amount) DESC
        """)
    List<CategorySumProjection> sumExpensesByCategory(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        SELECT t.category AS category, COALESCE(SUM(t.amount), 0) AS amount
        FROM Transaction t
        WHERE t.user.id = :userId AND t.type = com.flowiq.entity.Transaction$Type.REVENUE
        AND t.transactionDate BETWEEN :start AND :end
        GROUP BY t.category
        ORDER BY SUM(t.amount) DESC
        """)
    List<CategorySumProjection> sumRevenueByCategory(
            @Param("userId") Long userId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end
    );

    @Query("""
        SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Transaction t
        WHERE t.user.id = :userId
        AND t.transactionDate = :date
        AND t.amount = :amount
        AND t.type = :type
        AND LOWER(COALESCE(t.description, '')) = LOWER(COALESCE(:description, ''))
        """)
    boolean existsDuplicate(
            @Param("userId") Long userId,
            @Param("date") LocalDate date,
            @Param("amount") BigDecimal amount,
            @Param("type") Transaction.Type type,
            @Param("description") String description
    );

    interface CategorySumProjection {
        String getCategory();
        BigDecimal getAmount();
    }
}
