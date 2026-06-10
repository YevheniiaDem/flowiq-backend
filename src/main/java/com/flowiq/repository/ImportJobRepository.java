package com.flowiq.repository;

import com.flowiq.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImportJobRepository extends JpaRepository<ImportJob, Long> {

    List<ImportJob> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ImportJob> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    @Query("SELECT COALESCE(SUM(j.rowsImported), 0) FROM ImportJob j WHERE j.userId = :userId")
    long sumRowsImportedByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(j) FROM ImportJob j
        WHERE j.userId = :userId
        AND j.status IN (com.flowiq.entity.ImportJob$Status.COMPLETED, com.flowiq.entity.ImportJob$Status.PARTIAL)
        """)
    long countSuccessfulByUserId(@Param("userId") Long userId);

    Optional<ImportJob> findFirstByUserIdOrderByCreatedAtDesc(Long userId);
}
