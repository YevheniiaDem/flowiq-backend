package com.flowiq.tasks.repository;

import com.flowiq.tasks.entity.Task;
import com.flowiq.tasks.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndDeduplicationKey(Long userId, String deduplicationKey);

    @Query("""
            SELECT t FROM Task t
            WHERE t.userId = :userId
              AND t.status <> com.flowiq.tasks.entity.TaskStatus.COMPLETED
              AND t.dueDate = :date
            ORDER BY t.priority DESC, t.dueDate ASC
            """)
    List<Task> findTodayTasks(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("""
            SELECT t FROM Task t
            WHERE t.userId = :userId
              AND t.status <> com.flowiq.tasks.entity.TaskStatus.COMPLETED
              AND t.dueDate > :today
              AND t.dueDate <= :until
            ORDER BY t.dueDate ASC, t.priority DESC
            """)
    List<Task> findUpcomingTasks(
            @Param("userId") Long userId,
            @Param("today") LocalDate today,
            @Param("until") LocalDate until
    );

    @Query("""
            SELECT t FROM Task t
            WHERE t.userId = :userId
              AND t.status <> com.flowiq.tasks.entity.TaskStatus.COMPLETED
              AND t.dueDate < :today
            ORDER BY t.dueDate ASC, t.priority DESC
            """)
    List<Task> findOverdueTasks(@Param("userId") Long userId, @Param("today") LocalDate today);

    @Query("""
            SELECT t FROM Task t
            WHERE t.userId = :userId
              AND t.status = com.flowiq.tasks.entity.TaskStatus.COMPLETED
            ORDER BY t.completedAt DESC
            """)
    List<Task> findCompletedTasks(@Param("userId") Long userId);

    long countByUserIdAndStatusNot(Long userId, TaskStatus status);

    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.userId = :userId
              AND t.status <> com.flowiq.tasks.entity.TaskStatus.COMPLETED
              AND t.dueDate = :date
            """)
    long countTodayTasks(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.userId = :userId
              AND t.status <> com.flowiq.tasks.entity.TaskStatus.COMPLETED
              AND t.dueDate > :today
              AND t.dueDate <= :until
            """)
    long countUpcomingTasks(
            @Param("userId") Long userId,
            @Param("today") LocalDate today,
            @Param("until") LocalDate until
    );
}
