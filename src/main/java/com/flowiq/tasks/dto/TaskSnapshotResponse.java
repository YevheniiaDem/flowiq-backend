package com.flowiq.tasks.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Dashboard tasks snapshot widget")
public class TaskSnapshotResponse {

    private long todayCount;
    private long upcomingCount;
    private long overdueCount;
    private List<TaskResponse> todayTasks;
    private List<TaskResponse> upcomingDeadlines;
}
