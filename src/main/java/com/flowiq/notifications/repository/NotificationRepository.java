package com.flowiq.notifications.repository;

import com.flowiq.notifications.entity.Notification;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndDeduplicationKey(Long userId, String deduplicationKey);

    long countByUserId(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    long countByUserIdAndSeverity(Long userId, NotificationSeverity severity);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime after);

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.userId = :userId AND n.severity = com.flowiq.notifications.entity.NotificationSeverity.WARNING
        """)
    long countWarningsByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.userId = :userId AND n.severity = com.flowiq.notifications.entity.NotificationSeverity.SUCCESS
        """)
    long countSuccessByUserId(@Param("userId") Long userId);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("""
        UPDATE Notification n SET n.isRead = true, n.readAt = :readAt
        WHERE n.userId = :userId AND n.isRead = false
        """)
    int markAllReadByUserId(@Param("userId") Long userId, @Param("readAt") LocalDateTime readAt);

    @Query("""
        SELECT COUNT(n) FROM Notification n
        WHERE n.userId = :userId
        AND (:unreadOnly = false OR n.isRead = false)
        AND (:type IS NULL OR n.type = :type)
        AND (:severity IS NULL OR n.severity = :severity)
        """)
    long countFiltered(
            @Param("userId") Long userId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("type") NotificationType type,
            @Param("severity") NotificationSeverity severity
    );
}
