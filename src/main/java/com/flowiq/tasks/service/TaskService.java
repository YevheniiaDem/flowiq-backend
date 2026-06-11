package com.flowiq.tasks.service;

import com.flowiq.config.AppPreferences;
import com.flowiq.entity.Transaction;
import com.flowiq.entity.User;
import com.flowiq.exception.BadRequestException;
import com.flowiq.exception.ResourceNotFoundException;
import com.flowiq.exception.UnauthorizedException;
import com.flowiq.repository.TransactionRepository;
import com.flowiq.repository.UserRepository;
import com.flowiq.security.UserPrincipal;
import com.flowiq.service.TransactionSeedService;
import com.flowiq.tasks.dto.*;
import com.flowiq.tasks.entity.Task;
import com.flowiq.tasks.entity.TaskPriority;
import com.flowiq.tasks.entity.TaskStatus;
import com.flowiq.tasks.entity.TaskType;
import com.flowiq.tasks.repository.TaskRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final int UPCOMING_DAYS = 30;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "dueDate", "priority", "createdAt", "title", "status"
    );

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionSeedService transactionSeedService;
    private final TaskRuleEngine taskRuleEngine;

    @Transactional(readOnly = true)
    public TaskPageResponse getTasks(
            int page,
            int size,
            String search,
            TaskType type,
            TaskPriority priority,
            TaskStatus status,
            LocalDate dueDateFrom,
            LocalDate dueDateTo,
            String sort
    ) {
        User user = getCurrentUserEntity();
        ensureGeneratedTasks(user);
        Pageable pageable = buildPageable(page, size, sort);
        Specification<Task> spec = buildSpecification(
                user.getId(), search, type, priority, status, dueDateFrom, dueDateTo
        );
        Page<Task> result = taskRepository.findAll(spec, pageable);

        return TaskPageResponse.builder()
                .content(result.getContent().stream().map(TaskResponse::fromEntity).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTodayTasks() {
        User user = getCurrentUserEntity();
        ensureGeneratedTasks(user);
        LocalDate today = LocalDate.now();
        return taskRepository.findTodayTasks(user.getId(), today).stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getUpcomingTasks() {
        User user = getCurrentUserEntity();
        ensureGeneratedTasks(user);
        LocalDate today = LocalDate.now();
        return taskRepository.findUpcomingTasks(user.getId(), today, today.plusDays(UPCOMING_DAYS)).stream()
                .map(TaskResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskListResponse getGroupedTasks() {
        User user = getCurrentUserEntity();
        ensureGeneratedTasks(user);
        LocalDate today = LocalDate.now();

        return TaskListResponse.builder()
                .today(taskRepository.findTodayTasks(user.getId(), today).stream()
                        .map(TaskResponse::fromEntity).toList())
                .upcoming(taskRepository.findUpcomingTasks(user.getId(), today, today.plusDays(UPCOMING_DAYS))
                        .stream().map(TaskResponse::fromEntity).toList())
                .overdue(taskRepository.findOverdueTasks(user.getId(), today).stream()
                        .map(TaskResponse::fromEntity).toList())
                .completed(taskRepository.findCompletedTasks(user.getId()).stream()
                        .limit(20)
                        .map(TaskResponse::fromEntity).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<TaskSuggestionResponse> getSuggestions() {
        User user = getCurrentUserEntity();
        transactionSeedService.seedIfEmpty(user);
        boolean uk = AppPreferences.current().isUkrainian();
        LocalDate today = LocalDate.now();
        List<TaskSuggestionResponse> suggestions = new ArrayList<>();

        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);
        BigDecimal currentExpenses = sum(user.getId(), Transaction.Type.EXPENSE, current);
        BigDecimal previousExpenses = sum(user.getId(), Transaction.Type.EXPENSE, previous);
        BigDecimal currentRevenue = sum(user.getId(), Transaction.Type.REVENUE, current);
        BigDecimal previousRevenue = sum(user.getId(), Transaction.Type.REVENUE, previous);

        double expenseGrowth = percentChange(currentExpenses, previousExpenses);
        double revenueGrowth = percentChange(currentRevenue, previousRevenue);

        if (expenseGrowth > 10) {
            suggestions.add(TaskSuggestionResponse.builder()
                    .id("sug-expense-growth")
                    .title(uk ? "Переглянути зростання витрат" : "Review expense growth")
                    .description(uk
                            ? String.format(Locale.forLanguageTag("uk-UA"),
                            "Витрати зросли на %.1f%% порівняно з минулим місяцем", expenseGrowth)
                            : String.format(Locale.US,
                            "Expenses grew %.1f%% compared to last month", expenseGrowth))
                    .type(TaskType.BUSINESS)
                    .priority(TaskPriority.HIGH)
                    .suggestedDueDate(today.plusDays(5))
                    .build());
        }

        LocalDate nextTax = resolveNextTaxDeadline(today);
        int daysUntilTax = (int) ChronoUnit.DAYS.between(today, nextTax);
        if (daysUntilTax <= 30) {
            suggestions.add(TaskSuggestionResponse.builder()
                    .id("sug-tax-payment")
                    .title(uk ? "Підготувати податковий платіж" : "Prepare tax payment")
                    .description(uk
                            ? String.format("До сплати податку залишилось %d дн. (до %s)", daysUntilTax, nextTax)
                            : String.format("%d days until tax payment (due %s)", daysUntilTax, nextTax))
                    .type(TaskType.TAX)
                    .priority(daysUntilTax <= 7 ? TaskPriority.CRITICAL : TaskPriority.HIGH)
                    .suggestedDueDate(nextTax.minusDays(3))
                    .build());
        }

        suggestions.add(TaskSuggestionResponse.builder()
                .id("sug-monthly-report")
                .title(uk ? "Згенерувати місячний звіт" : "Generate monthly report")
                .description(uk
                        ? "Створіть звіт про прибутки та збитки за поточний місяць"
                        : "Create a profit & loss report for the current month")
                .type(TaskType.REPORTING)
                .priority(TaskPriority.MEDIUM)
                .suggestedDueDate(today.plusDays(7))
                .build());

        if (revenueGrowth < -5) {
            suggestions.add(TaskSuggestionResponse.builder()
                    .id("sug-revenue-decline")
                    .title(uk ? "Проаналізувати зниження доходу" : "Analyze revenue decline")
                    .description(uk
                            ? String.format("Дохід знизився на %.1f%% — перевірте канали продажів", Math.abs(revenueGrowth))
                            : String.format("Revenue declined %.1f%% — review sales channels", Math.abs(revenueGrowth)))
                    .type(TaskType.BUSINESS)
                    .priority(TaskPriority.HIGH)
                    .suggestedDueDate(today.plusDays(3))
                    .build());
        }

        return suggestions;
    }

    @Transactional(readOnly = true)
    public TaskSnapshotResponse getSnapshot() {
        User user = getCurrentUserEntity();
        ensureGeneratedTasks(user);
        LocalDate today = LocalDate.now();
        LocalDate until = today.plusDays(UPCOMING_DAYS);

        List<TaskResponse> todayTasks = taskRepository.findTodayTasks(user.getId(), today).stream()
                .limit(5)
                .map(TaskResponse::fromEntity)
                .toList();
        List<TaskResponse> upcoming = taskRepository.findUpcomingTasks(user.getId(), today, until).stream()
                .limit(5)
                .map(TaskResponse::fromEntity)
                .toList();

        return TaskSnapshotResponse.builder()
                .todayCount(taskRepository.countTodayTasks(user.getId(), today))
                .upcomingCount(taskRepository.countUpcomingTasks(user.getId(), today, until))
                .overdueCount(taskRepository.findOverdueTasks(user.getId(), today).size())
                .todayTasks(todayTasks)
                .upcomingDeadlines(upcoming)
                .build();
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        User user = getCurrentUserEntity();
        validateStatus(request.getStatus());

        Task task = new Task();
        task.setUserId(user.getId());
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setType(request.getType() != null ? request.getType() : TaskType.CUSTOM);
        task.setPriority(request.getPriority() != null ? request.getPriority() : TaskPriority.MEDIUM);
        task.setStatus(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO);
        task.setDueDate(request.getDueDate());

        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        User user = getCurrentUserEntity();
        Task task = findOwnedTask(id, user.getId());

        if (request.getTitle() != null) {
            task.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            task.setType(request.getType());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            validateStatus(request.getStatus());
            applyStatus(task, request.getStatus());
        }
        if (request.getDueDate() != null) {
            task.setDueDate(request.getDueDate());
        }

        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse complete(Long id) {
        User user = getCurrentUserEntity();
        Task task = findOwnedTask(id, user.getId());
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        return TaskResponse.fromEntity(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        User user = getCurrentUserEntity();
        Task task = findOwnedTask(id, user.getId());
        taskRepository.delete(task);
    }

    private void ensureGeneratedTasks(User user) {
        transactionSeedService.seedIfEmpty(user);
        taskRuleEngine.generateForUser(user);
    }

    private Task findOwnedTask(Long id, Long userId) {
        return taskRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
    }

    private void validateStatus(TaskStatus status) {
        if (status == TaskStatus.OVERDUE) {
            throw new BadRequestException("OVERDUE status is computed automatically");
        }
    }

    private void applyStatus(Task task, TaskStatus status) {
        if (status == TaskStatus.COMPLETED) {
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setCompletedAt(null);
        }
        task.setStatus(status);
    }

    private Pageable buildPageable(int page, int size, String sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        if (sort == null || sort.isBlank()) {
            return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "dueDate"));
        }

        String[] parts = sort.split(",");
        String field = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            field = "dueDate";
        }
        Sort.Direction direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        return PageRequest.of(safePage, safeSize, Sort.by(direction, field));
    }

    private Specification<Task> buildSpecification(
            Long userId,
            String search,
            TaskType type,
            TaskPriority priority,
            TaskStatus status,
            LocalDate dueDateFrom,
            LocalDate dueDateTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (status != null) {
                LocalDate today = LocalDate.now();
                if (status == TaskStatus.OVERDUE) {
                    predicates.add(cb.notEqual(root.get("status"), TaskStatus.COMPLETED));
                    predicates.add(cb.lessThan(root.get("dueDate"), today));
                } else if (status == TaskStatus.COMPLETED) {
                    predicates.add(cb.equal(root.get("status"), TaskStatus.COMPLETED));
                } else {
                    predicates.add(cb.equal(root.get("status"), status));
                    predicates.add(cb.or(
                            cb.isNull(root.get("dueDate")),
                            cb.greaterThanOrEqualTo(root.get("dueDate"), today)
                    ));
                }
            }
            if (dueDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueDateFrom));
            }
            if (dueDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueDateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private User getCurrentUserEntity() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Not authenticated");
        }
        return userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }

    private BigDecimal sum(Long userId, Transaction.Type type, YearMonth month) {
        return transactionRepository.sumByUserAndTypeAndDateRange(
                userId, type, month.atDay(1), month.atEndOfMonth());
    }

    private double percentChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    private LocalDate resolveNextTaxDeadline(LocalDate today) {
        List<LocalDate> deadlines = List.of(
                LocalDate.of(today.getYear(), 5, 10),
                LocalDate.of(today.getYear(), 8, 9),
                LocalDate.of(today.getYear(), 11, 9),
                LocalDate.of(today.getYear() + 1, 2, 9)
        );
        for (LocalDate deadline : deadlines) {
            if (!deadline.isBefore(today)) {
                return deadline;
            }
        }
        return LocalDate.of(today.getYear() + 1, 5, 10);
    }
}
