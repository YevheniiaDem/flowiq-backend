package com.flowiq.tasks.service;

import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.service.NotificationGeneratorService;
import com.flowiq.tasks.entity.Task;
import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskStatus;
import com.flowiq.tasks.entity.TaskType;
import com.flowiq.tasks.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TaskGeneratorService {

    private final TaskRepository taskRepository;
    private final NotificationGeneratorService notificationGenerator;

    @Transactional
    public Task createIfAbsent(
            Long userId,
            String deduplicationKey,
            String title,
            String description,
            TaskType type,
            TaskPriority priority,
            LocalDate dueDate,
            boolean createNotification,
            String notificationTitle,
            String notificationMessage,
            NotificationType notificationType,
            NotificationSeverity notificationSeverity
    ) {
        if (taskRepository.existsByUserIdAndDeduplicationKey(userId, deduplicationKey)) {
            return null;
        }

        Task task = new Task();
        task.setUserId(userId);
        task.setDeduplicationKey(deduplicationKey);
        task.setTitle(title);
        task.setDescription(description);
        task.setType(type);
        task.setPriority(priority);
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(dueDate);

        Task saved = taskRepository.save(task);

        if (createNotification) {
            notificationGenerator.createIfAbsent(
                    userId,
                    "task-" + deduplicationKey,
                    notificationTitle != null ? notificationTitle : title,
                    notificationMessage != null ? notificationMessage : description,
                    notificationType,
                    notificationSeverity,
                    "/tasks",
                    dueDate != null ? dueDate.atTime(23, 59) : null
            );
        }

        return saved;
    }

    @Transactional
    public void createImportReviewTask(Long userId, Long jobId, int rowsImported) {
        createIfAbsent(
                userId,
                "import-review-" + jobId,
                "Перевірити імпорт CSV",
                String.format("Перегляньте %d імпортованих транзакцій та підтвердьте категорії", rowsImported),
                TaskType.SYSTEM,
                TaskPriority.MEDIUM,
                LocalDate.now().plusDays(3),
                true,
                "Нове завдання: перевірка імпорту",
                String.format("Перевірте %d імпортованих транзакцій", rowsImported),
                NotificationType.SYSTEM,
                NotificationSeverity.INFO
        );
    }

    @Transactional
    public void createFromNotification(
            Long userId,
            String notificationDedupKey,
            String title,
            String message,
            TaskType type,
            TaskPriority priority,
            LocalDate dueDate
    ) {
        createIfAbsent(
                userId,
                "notif-" + notificationDedupKey,
                title,
                message,
                type,
                priority,
                dueDate,
                false,
                null,
                null,
                null,
                null
        );
    }

    @Transactional
    public void createReportReviewTask(Long userId, Long jobId, String fileName) {
        createIfAbsent(
                userId,
                "report-review-" + jobId,
                "Переглянути звіт",
                "Звіт сформовано: " + fileName + ". Перевірте дані та завантажте при потребі.",
                TaskType.REPORTING,
                TaskPriority.MEDIUM,
                LocalDate.now().plusDays(7),
                true,
                "Нове завдання: перегляд звіту",
                "Звіт готовий до перегляду: " + fileName,
                NotificationType.REPORT,
                NotificationSeverity.INFO
        );
    }
}
