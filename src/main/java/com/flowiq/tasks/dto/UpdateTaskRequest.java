package com.flowiq.tasks.dto;

import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskStatus;
import com.flowiq.tasks.entity.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Request to update a task")
public class UpdateTaskRequest {

    @Size(max = 255)
    private String title;

    @Size(max = 1000)
    private String description;

    private TaskType type;

    private TaskPriority priority;

    private TaskStatus status;

    private LocalDate dueDate;
}
