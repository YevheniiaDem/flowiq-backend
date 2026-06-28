package com.flowiq.tasks.controller;

import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.tasks.dto.CreateTaskRequest;
import com.flowiq.tasks.dto.TaskListResponse;
import com.flowiq.tasks.dto.TaskPageResponse;
import com.flowiq.tasks.dto.TaskResponse;
import com.flowiq.tasks.dto.TaskSuggestionResponse;
import com.flowiq.tasks.dto.UpdateTaskRequest;
import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskStatus;
import com.flowiq.tasks.entity.TaskType;
import com.flowiq.tasks.service.TaskService;
import com.flowiq.unit.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("TaskController tests")
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = ControllerTestSupport.standaloneMockMvc(taskController);
    }

    @Test
    @DisplayName("GET /api/tasks returns paginated tasks")
    void list_success() throws Exception {
        when(taskService.getTasks(eq(0), eq(20), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(TaskPageResponse.builder().content(List.of()).page(0).size(20).totalElements(0).build());

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    @DisplayName("GET /api/tasks/today returns today's tasks")
    void today_success() throws Exception {
        when(taskService.getTodayTasks()).thenReturn(List.of(
                TaskResponse.builder().id(1L).title("Pay ESV").status(TaskStatus.TODO).build()));

        mockMvc.perform(get("/api/tasks/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Pay ESV"));
    }

    @Test
    @DisplayName("GET /api/tasks/upcoming returns upcoming tasks")
    void upcoming_success() throws Exception {
        when(taskService.getUpcomingTasks()).thenReturn(List.of(
                TaskResponse.builder().id(2L).title("File declaration").build()));

        mockMvc.perform(get("/api/tasks/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    @DisplayName("GET /api/tasks/grouped returns grouped tasks")
    void grouped_success() throws Exception {
        when(taskService.getGroupedTasks()).thenReturn(TaskListResponse.builder().build());

        mockMvc.perform(get("/api/tasks/grouped"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/tasks/suggestions returns AI suggestions")
    void suggestions_success() throws Exception {
        when(taskService.getSuggestions()).thenReturn(List.of(
                TaskSuggestionResponse.builder().title("Review tax payments").build()));

        mockMvc.perform(get("/api/tasks/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Review tax payments"));
    }

    @Test
    @DisplayName("POST /api/tasks creates task")
    void create_success() throws Exception {
        CreateTaskRequest request = validCreateRequest();

        when(taskService.create(any(CreateTaskRequest.class)))
                .thenReturn(TaskResponse.builder().id(1L).title("Pay ESV").type(TaskType.TAX).build());

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/tasks rejects blank title")
    void create_validationError() throws Exception {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("   ");

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/tasks/{id} updates task")
    void update_success() throws Exception {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Updated task");
        request.setPriority(TaskPriority.HIGH);

        when(taskService.update(eq(1L), any(UpdateTaskRequest.class)))
                .thenReturn(TaskResponse.builder().id(1L).title("Updated task").build());

        mockMvc.perform(put("/api/tasks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated task"));
    }

    @Test
    @DisplayName("PUT /api/tasks/{id}/complete marks task complete")
    void complete_success() throws Exception {
        when(taskService.complete(1L))
                .thenReturn(TaskResponse.builder().id(1L).status(TaskStatus.COMPLETED).build());

        mockMvc.perform(put("/api/tasks/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("DELETE /api/tasks/{id} returns 204")
    void delete_success() throws Exception {
        mockMvc.perform(delete("/api/tasks/1"))
                .andExpect(status().isNoContent());

        verify(taskService).delete(1L);
    }

    @Test
    @DisplayName("GET /api/tasks/{id} path returns 404 when service throws not found on update")
    void update_notFound() throws Exception {
        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Updated");

        when(taskService.update(eq(99L), any(UpdateTaskRequest.class)))
                .thenThrow(new ResourceNotFoundException("Task not found"));

        mockMvc.perform(put("/api/tasks/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ControllerTestSupport.OBJECT_MAPPER.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/tasks returns 401 when unauthorized")
    void list_unauthorized() throws Exception {
        when(taskService.getTasks(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new UnauthorizedException("Not authenticated"));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized());
    }

    private CreateTaskRequest validCreateRequest() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Pay ESV");
        request.setType(TaskType.TAX);
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.of(2026, 6, 15));
        return request;
    }
}
