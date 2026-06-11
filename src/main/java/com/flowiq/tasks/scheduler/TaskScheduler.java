package com.flowiq.tasks.scheduler;

import com.flowiq.entity.User;
import com.flowiq.repository.UserRepository;
import com.flowiq.tasks.service.TaskRuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TaskScheduler {

    private final UserRepository userRepository;
    private final TaskRuleEngine taskRuleEngine;

    @Scheduled(cron = "0 30 7 * * *")
    public void generateDailyTasks() {
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .toList();

        for (User user : activeUsers) {
            try {
                taskRuleEngine.generateForUser(user);
            } catch (Exception e) {
                log.warn("Failed to generate tasks for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }
}
