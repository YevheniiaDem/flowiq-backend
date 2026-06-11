package com.flowiq.tasks.dto;

import com.flowiq.tasks.entity.Task;
import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskStatus;
import com.flowiq.tasks.entity.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Task response")
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private TaskType type;
    private TaskPriority priority;
    private TaskStatus status;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private boolean overdue;

    public static TaskResponse fromEntity(Task task) {
        TaskStatus effectiveStatus = resolveEffectiveStatus(task);
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .type(task.getType())
                .priority(task.getPriority())
                .status(effectiveStatus)
                .dueDate(task.getDueDate())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .overdue(effectiveStatus == TaskStatus.OVERDUE)
                .build();
    }

    public static TaskStatus resolveEffectiveStatus(Task task) {
        if (task.getStatus() == TaskStatus.COMPLETED) {
            return TaskStatus.COMPLETED;
        }
        if (task.getDueDate() != null
                && task.getDueDate().isBefore(LocalDate.now())
                && task.getStatus() != TaskStatus.COMPLETED) {
            return TaskStatus.OVERDUE;
        }
        return task.getStatus();
    }
}
