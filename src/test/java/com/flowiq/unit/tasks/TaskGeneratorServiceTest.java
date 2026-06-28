package com.flowiq.unit.tasks;

import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.preferences.NotificationPreferenceKey;
import com.flowiq.notifications.service.NotificationGeneratorService;
import com.flowiq.tasks.entity.Task;
import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskStatus;
import com.flowiq.tasks.entity.TaskType;
import com.flowiq.tasks.repository.TaskRepository;
import com.flowiq.tasks.service.TaskGeneratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskGeneratorService unit tests")
class TaskGeneratorServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private NotificationGeneratorService notificationGenerator;

    @InjectMocks
    private TaskGeneratorService taskGeneratorService;

    @Test
    @DisplayName("createIfAbsent saves task and notification when absent")
    void createIfAbsent_success() {
        when(taskRepository.existsByUserIdAndDeduplicationKey(1L, "key-1")).thenReturn(false);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task task = inv.getArgument(0);
            task.setId(10L);
            return task;
        });

        Task saved = taskGeneratorService.createIfAbsent(
                1L, "key-1", "Title", "Description",
                TaskType.SYSTEM, TaskPriority.HIGH, LocalDate.now().plusDays(1),
                true, "Notif title", "Notif message",
                NotificationType.SYSTEM, NotificationSeverity.INFO,
                NotificationPreferenceKey.IMPORT_COMPLETED
        );

        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.TODO);
        verify(notificationGenerator).createIfAbsent(
                eq(1L), eq("task-key-1"), eq("Notif title"), eq("Notif message"),
                eq(NotificationType.SYSTEM), eq(NotificationSeverity.INFO),
                eq("/tasks"), any(), eq(NotificationPreferenceKey.IMPORT_COMPLETED));
    }

    @Test
    @DisplayName("createIfAbsent returns null when deduplication key exists")
    void createIfAbsent_duplicate() {
        when(taskRepository.existsByUserIdAndDeduplicationKey(1L, "key-1")).thenReturn(true);

        Task result = taskGeneratorService.createIfAbsent(
                1L, "key-1", "Title", "Description",
                TaskType.CUSTOM, TaskPriority.MEDIUM, null,
                true, null, null, null, null, NotificationPreferenceKey.IMPORT_COMPLETED
        );

        assertThat(result).isNull();
        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("createImportReviewTask creates import review task")
    void createImportReviewTask() {
        when(taskRepository.existsByUserIdAndDeduplicationKey(any(), any())).thenReturn(false);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        taskGeneratorService.createImportReviewTask(1L, 42L, 15);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getDeduplicationKey()).isEqualTo("import-review-42");
        assertThat(captor.getValue().getType()).isEqualTo(TaskType.SYSTEM);
    }

    @Test
    @DisplayName("createReportReviewTask creates report review task")
    void createReportReviewTask() {
        when(taskRepository.existsByUserIdAndDeduplicationKey(any(), any())).thenReturn(false);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        taskGeneratorService.createReportReviewTask(1L, 7L, "report.pdf");

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).contains("звіт");
    }
}
