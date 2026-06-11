package com.flowiq.tasks.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "Paginated task list")
public class TaskPageResponse {

    private List<TaskResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
