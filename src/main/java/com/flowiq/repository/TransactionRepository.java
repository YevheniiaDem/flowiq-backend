package com.flowiq.repository;

import com.flowiq.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    boolean existsByUserId(Long userId);

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

    interface CategorySumProjection {
        String getCategory();
        BigDecimal getAmount();
    }
}
