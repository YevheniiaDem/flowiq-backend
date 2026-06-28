package com.flowiq.integration.repository;

import com.flowiq.entity.User;
import com.flowiq.integration.support.AbstractPostgresIntegrationTest;
import com.flowiq.repository.UserRepository;
import com.flowiq.tasks.entity.Task;
import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskStatus;
import com.flowiq.tasks.entity.TaskType;
import com.flowiq.tasks.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TaskRepository integration tests")
class TaskRepositoryTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setEmail("repo-task-" + System.nanoTime() + "@test.flowiq");
        user.setPassword("encoded");
        user.setName("Task User");
        user.setRole(User.Role.USER);
        user.setActive(true);
        user = userRepository.save(user);
    }

    @Test
    @DisplayName("save and findByIdAndUserId returns persisted task")
    @Transactional
    void save_andFindByIdAndUserId() {
        Task saved = taskRepository.save(sampleTask("File taxes", LocalDate.of(2026, 6, 15)));

        assertThat(taskRepository.findByIdAndUserId(saved.getId(), user.getId()))
                .isPresent()
                .get()
                .extracting(Task::getTitle, Task::getUserId)
                .containsExactly("File taxes", user.getId());
    }

    @Test
    @DisplayName("findTodayTasks returns open tasks due today")
    @Transactional
    void findTodayTasks_returnsTasksDueToday() {
        LocalDate today = LocalDate.of(2026, 6, 20);
        taskRepository.save(sampleTask("Today task", today));
        taskRepository.save(sampleTask("Future task", today.plusDays(3)));
        taskRepository.save(completedTask("Done today", today));

        List<Task> todayTasks = taskRepository.findTodayTasks(user.getId(), today);

        assertThat(todayTasks).hasSize(1);
        assertThat(todayTasks.get(0).getTitle()).isEqualTo("Today task");
    }

    @Test
    @DisplayName("existsByUserIdAndDeduplicationKey detects duplicate task")
    @Transactional
    void existsByUserIdAndDeduplicationKey_detectsDuplicate() {
        String key = "dedup-" + System.nanoTime();
        taskRepository.save(sampleTask("Dedup task", LocalDate.of(2026, 6, 1), key));

        assertThat(taskRepository.existsByUserIdAndDeduplicationKey(user.getId(), key)).isTrue();
        assertThat(taskRepository.existsByUserIdAndDeduplicationKey(user.getId(), "other-key")).isFalse();
    }

    private Task sampleTask(String title, LocalDate dueDate) {
        return sampleTask(title, dueDate, "key-" + System.nanoTime());
    }

    private Task sampleTask(String title, LocalDate dueDate, String deduplicationKey) {
        Task task = new Task();
        task.setUserId(user.getId());
        task.setTitle(title);
        task.setType(TaskType.TAX);
        task.setPriority(TaskPriority.HIGH);
        task.setStatus(TaskStatus.TODO);
        task.setDueDate(dueDate);
        task.setDeduplicationKey(deduplicationKey);
        return task;
    }

    private Task completedTask(String title, LocalDate dueDate) {
        Task task = sampleTask(title, dueDate);
        task.setStatus(TaskStatus.COMPLETED);
        return task;
    }
}
