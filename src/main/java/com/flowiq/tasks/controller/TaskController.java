package com.flowiq.tasks.controller;

import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.tasks.dto.*;
import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskStatus;
import com.flowiq.tasks.entity.TaskType;
import com.flowiq.tasks.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Tasks", description = "Tasks & Deadlines Center for FOP business planning")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "List tasks", description = "Returns paginated tasks with search and filters.")
    @ApiResponse(responseCode = "200", description = "Paginated task list",
            content = @Content(schema = @Schema(implementation = TaskPageResponse.class)))
    @ApiErrorResponses
    @GetMapping
    public ResponseEntity<TaskPageResponse> getTasks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TaskType type,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(taskService.getTasks(
                page, size, search, type, priority, status, dueDateFrom, dueDateTo, sort
        ));
    }

    @Operation(summary = "Today's tasks")
    @ApiResponse(responseCode = "200", description = "Tasks due today",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class))))
    @ApiErrorResponses
    @GetMapping("/today")
    public ResponseEntity<List<TaskResponse>> getTodayTasks() {
        return ResponseEntity.ok(taskService.getTodayTasks());
    }

    @Operation(summary = "Upcoming tasks")
    @ApiResponse(responseCode = "200", description = "Tasks due in the next 30 days",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskResponse.class))))
    @ApiErrorResponses
    @GetMapping("/upcoming")
    public ResponseEntity<List<TaskResponse>> getUpcomingTasks() {
        return ResponseEntity.ok(taskService.getUpcomingTasks());
    }

    @Operation(summary = "Grouped tasks", description = "Returns tasks grouped by today, upcoming, overdue, and completed.")
    @ApiResponse(responseCode = "200", description = "Grouped task lists",
            content = @Content(schema = @Schema(implementation = TaskListResponse.class)))
    @ApiErrorResponses
    @GetMapping("/grouped")
    public ResponseEntity<TaskListResponse> getGroupedTasks() {
        return ResponseEntity.ok(taskService.getGroupedTasks());
    }

    @Operation(summary = "AI task suggestions")
    @ApiResponse(responseCode = "200", description = "Rule-based task suggestions",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = TaskSuggestionResponse.class))))
    @ApiErrorResponses
    @GetMapping("/suggestions")
    public ResponseEntity<List<TaskSuggestionResponse>> getSuggestions() {
        return ResponseEntity.ok(taskService.getSuggestions());
    }

    @Operation(summary = "Create task")
    @ApiResponse(responseCode = "201", description = "Task created",
            content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiErrorResponses
    @PostMapping
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @Operation(summary = "Update task")
    @ApiResponse(responseCode = "200", description = "Task updated",
            content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiErrorResponses
    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> update(
            @Parameter(description = "Task ID") @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @Operation(summary = "Complete task")
    @ApiResponse(responseCode = "200", description = "Task completed",
            content = @Content(schema = @Schema(implementation = TaskResponse.class)))
    @ApiErrorResponses
    @PutMapping("/{id}/complete")
    public ResponseEntity<TaskResponse> complete(
            @Parameter(description = "Task ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(taskService.complete(id));
    }

    @Operation(summary = "Delete task")
    @ApiResponse(responseCode = "204", description = "Task deleted")
    @ApiErrorResponses
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Task ID") @PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
