package com.flowiq.unit.tasks;

import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.service.TransactionSeedService;
import com.flowiq.tasks.dto.CreateTaskRequest;
import com.flowiq.tasks.dto.UpdateTaskRequest;
import com.flowiq.tasks.entity.Task;
import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskStatus;
import com.flowiq.tasks.entity.TaskType;
import com.flowiq.tasks.repository.TaskRepository;
import com.flowiq.tasks.service.TaskRuleEngine;
import com.flowiq.tasks.service.TaskService;
import com.flowiq.unit.support.SecurityTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskService unit tests")
class TaskServiceTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "tasks@test.flowiq";

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionSeedService transactionSeedService;
    @Mock
    private TaskRuleEngine taskRuleEngine;

    @InjectMocks
    private TaskService taskService;

    private User user;

    @BeforeEach
    void setUp() {
        user = SecurityTestSupport.testUser(USER_ID, EMAIL);
        SecurityTestSupport.authenticate(user);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(transactionRepository.sumByUserAndTypeAndDateRange(
                anyLong(), any(Transaction.Type.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(taskRepository.findTodayTasks(USER_ID, LocalDate.now())).thenReturn(List.of());
        when(taskRepository.findUpcomingTasks(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(taskRepository.findOverdueTasks(USER_ID, LocalDate.now())).thenReturn(List.of());
        when(taskRepository.findCompletedTasks(USER_ID)).thenReturn(List.of());
        when(taskRepository.countTodayTasks(anyLong(), any(LocalDate.class))).thenReturn(0L);
        when(taskRepository.countUpcomingTasks(anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(0L);
    }

    @AfterEach
    void tearDown() {
        SecurityTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("getTasks returns paginated tasks")
    void getTasks_success() {
        Task task = sampleTask(1L, "Review expenses");
        Page<Task> page = new PageImpl<>(List.of(task));
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var response = taskService.getTasks(0, 10, null, null, null, null, null, null, null);

        assertThat(response.getContent()).hasSize(1);
        verify(taskRuleEngine).generateForUser(user);
    }

    @Test
    @DisplayName("getSuggestions always includes monthly report suggestion")
    void getSuggestions_success() {
        var suggestions = taskService.getSuggestions();

        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions.stream().map(s -> s.getId()))
                .contains("sug-monthly-report");
    }

    @Test
    @DisplayName("create persists custom task")
    void create_success() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Pay taxes");
        request.setType(TaskType.TAX);
        request.setPriority(TaskPriority.HIGH);
        request.setDueDate(LocalDate.now().plusDays(7));

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        var response = taskService.create(request);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getTitle()).isEqualTo("Pay taxes");
    }

    @Test
    @DisplayName("create rejects OVERDUE status")
    void create_rejectsOverdueStatus() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTitle("Invalid");
        request.setStatus(TaskStatus.OVERDUE);

        assertThatThrownBy(() -> taskService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("OVERDUE status is computed automatically");
    }

    @Test
    @DisplayName("update modifies owned task")
    void update_success() {
        Task task = sampleTask(3L, "Old title");
        when(taskRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        UpdateTaskRequest request = new UpdateTaskRequest();
        request.setTitle("Updated title");
        request.setPriority(TaskPriority.CRITICAL);

        var response = taskService.update(3L, request);

        assertThat(response.getTitle()).isEqualTo("Updated title");
        assertThat(response.getPriority()).isEqualTo(TaskPriority.CRITICAL);
    }

    @Test
    @DisplayName("update throws when task not found")
    void update_notFound() {
        when(taskRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.update(99L, new UpdateTaskRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Task not found");
    }

    @Test
    @DisplayName("complete marks task as completed")
    void complete_success() {
        Task task = sampleTask(4L, "Finish report");
        when(taskRepository.findByIdAndUserId(4L, USER_ID)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        var response = taskService.complete(4L);

        assertThat(response.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(response.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("delete removes owned task")
    void delete_success() {
        Task task = sampleTask(6L, "Remove me");
        when(taskRepository.findByIdAndUserId(6L, USER_ID)).thenReturn(Optional.of(task));

        taskService.delete(6L);

        verify(taskRepository).delete(task);
    }

    @Test
    @DisplayName("rejects unauthenticated access")
    void rejectsUnauthenticated() {
        SecurityTestSupport.clearSecurityContext();

        assertThatThrownBy(() -> taskService.getTodayTasks())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Not authenticated");
    }

    private Task sampleTask(Long id, String title) {
        Task task = new Task();
        task.setId(id);
        task.setUserId(USER_ID);
        task.setTitle(title);
        task.setType(TaskType.CUSTOM);
        task.setPriority(TaskPriority.MEDIUM);
        task.setStatus(TaskStatus.TODO);
        return task;
    }
}
