package com.flowiq.tasks.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Grouped task lists for planner views")
public class TaskListResponse {

    private List<TaskResponse> today;
    private List<TaskResponse> upcoming;
    private List<TaskResponse> overdue;
    private List<TaskResponse> completed;
}
