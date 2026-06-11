package com.flowiq.tasks.dto;

import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
@Schema(description = "AI-suggested task")
public class TaskSuggestionResponse {

    private String id;
    private String title;
    private String description;
    private TaskType type;
    private TaskPriority priority;
    private LocalDate suggestedDueDate;
}
