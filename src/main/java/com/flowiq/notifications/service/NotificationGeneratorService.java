package com.flowiq.notifications.service;

import com.flowiq.notifications.entity.Notification;
import com.flowiq.notifications.entity.NotificationChannel;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationGeneratorService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void createIfAbsent(
            Long userId,
            String deduplicationKey,
            String title,
            String message,
            NotificationType type,
            NotificationSeverity severity,
            String actionUrl,
            LocalDateTime expiresAt
    ) {
        if (notificationRepository.existsByUserIdAndDeduplicationKey(userId, deduplicationKey)) {
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setDeduplicationKey(deduplicationKey);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setSeverity(severity);
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setActionUrl(actionUrl);
        notification.setExpiresAt(expiresAt);
        notification.setRead(false);

        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyReportCompleted(Long userId, Long jobId, String fileName) {
        createIfAbsent(
                userId,
                "report-completed-" + jobId,
                "Звіт сформовано",
                "Звіт успішно сформовано: " + fileName,
                NotificationType.REPORT,
                NotificationSeverity.SUCCESS,
                "/reports",
                LocalDateTime.now().plusDays(30)
        );
    }

    @Transactional
    public void notifyImportCompleted(Long userId, Long jobId, int rowsImported) {
        createIfAbsent(
                userId,
                "import-completed-" + jobId,
                "Імпорт завершено",
                String.format("Імпортовано %d транзакцій", rowsImported),
                NotificationType.SYSTEM,
                NotificationSeverity.SUCCESS,
                "/imports",
                LocalDateTime.now().plusDays(30)
        );
    }
}
