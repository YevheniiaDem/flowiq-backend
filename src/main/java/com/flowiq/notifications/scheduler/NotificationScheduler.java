package com.flowiq.notifications.scheduler;

import com.flowiq.entity.User;
import com.flowiq.notifications.service.NotificationRuleEngine;
import com.flowiq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationScheduler {

    private final UserRepository userRepository;
    private final NotificationRuleEngine notificationRuleEngine;

    @Scheduled(cron = "0 0 8 * * *")
    public void generateDailyNotifications() {
        log.info("Starting daily notification generation");
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(User::isActive)
                .toList();

        for (User user : activeUsers) {
            try {
                notificationRuleEngine.generateForUser(user);
            } catch (Exception e) {
                log.warn("Failed to generate notifications for user {}: {}", user.getId(), e.getMessage());
            }
        }

        log.info("Completed daily notification generation for {} users", activeUsers.size());
    }
}
