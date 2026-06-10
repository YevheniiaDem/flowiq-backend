package com.flowiq.repository;

import com.flowiq.entity.ReportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportJobRepository extends JpaRepository<ReportJob, Long> {

    List<ReportJob> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ReportJob> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    Optional<ReportJob> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
        SELECT r.reportType FROM ReportJob r
        WHERE r.userId = :userId
        GROUP BY r.reportType
        ORDER BY COUNT(r) DESC
        LIMIT 1
        """)
    Optional<ReportJob.ReportType> findMostUsedReportType(@Param("userId") Long userId);
}
