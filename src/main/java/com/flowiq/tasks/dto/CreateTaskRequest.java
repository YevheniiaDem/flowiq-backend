package com.flowiq.tasks.dto;

import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskStatus;
import com.flowiq.tasks.entity.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Request to create a custom task")
public class CreateTaskRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String description;

    private TaskType type = TaskType.CUSTOM;

    private TaskPriority priority = TaskPriority.MEDIUM;

    private TaskStatus status = TaskStatus.TODO;

    private LocalDate dueDate;
}
