package com.flowiq.notifications.service;

import com.flowiq.notifications.entity.Notification;
import com.flowiq.notifications.entity.NotificationChannel;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.preferences.NotificationPreferenceKey;
import com.flowiq.notifications.preferences.NotificationPreferenceService;
import com.flowiq.notifications.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationGeneratorService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceService preferenceService;

    @Transactional
    public void createIfAbsent(
            Long userId,
            String deduplicationKey,
            String title,
            String message,
            NotificationType type,
            NotificationSeverity severity,
            String actionUrl,
            LocalDateTime expiresAt,
            NotificationPreferenceKey preferenceKey
    ) {
        if (!preferenceService.isInAppEnabled(userId, preferenceKey)) {
            return;
        }
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
    public void notifyReportCompleted(Long userId, Long jobId, String fileName, String format) {
        createIfAbsent(
                userId,
                "report-completed-" + jobId,
                "Звіт сформовано",
                "Звіт успішно сформовано: " + fileName,
                NotificationType.REPORT,
                NotificationSeverity.SUCCESS,
                "/reports",
                LocalDateTime.now().plusDays(30),
                NotificationPreferenceKey.REPORT_READY
        );

        if ("PDF".equalsIgnoreCase(format)) {
            createIfAbsent(
                    userId,
                    "report-pdf-" + jobId,
                    "PDF звіт готовий",
                    "PDF звіт доступний для завантаження: " + fileName,
                    NotificationType.REPORT,
                    NotificationSeverity.SUCCESS,
                    "/reports",
                    LocalDateTime.now().plusDays(30),
                    NotificationPreferenceKey.REPORT_PDF_AVAILABLE
            );
        } else if ("XLSX".equalsIgnoreCase(format) || "EXCEL".equalsIgnoreCase(format)) {
            createIfAbsent(
                    userId,
                    "report-excel-" + jobId,
                    "Excel звіт готовий",
                    "Excel звіт доступний для завантаження: " + fileName,
                    NotificationType.REPORT,
                    NotificationSeverity.SUCCESS,
                    "/reports",
                    LocalDateTime.now().plusDays(30),
                    NotificationPreferenceKey.REPORT_EXCEL_AVAILABLE
            );
        }
    }

    @Transactional
    public void notifyReportFailed(Long userId, Long jobId, String reportType) {
        createIfAbsent(
                userId,
                "report-failed-" + jobId,
                "Помилка генерації звіту",
                "Не вдалося сформувати звіт: " + reportType,
                NotificationType.REPORT,
                NotificationSeverity.CRITICAL,
                "/reports",
                LocalDateTime.now().plusDays(14),
                NotificationPreferenceKey.REPORT_GENERATION_ERROR
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
                LocalDateTime.now().plusDays(30),
                NotificationPreferenceKey.IMPORT_COMPLETED
        );
    }

    @Transactional
    public void notifyImportFailed(Long userId, Long jobId, String fileName) {
        createIfAbsent(
                userId,
                "import-failed-" + jobId,
                "Імпорт не вдався",
                "Не вдалося імпортувати файл: " + fileName,
                NotificationType.SYSTEM,
                NotificationSeverity.CRITICAL,
                "/imports",
                LocalDateTime.now().plusDays(14),
                NotificationPreferenceKey.IMPORT_FAILED
        );
    }

    @Transactional
    public void notifyImportPartial(Long userId, Long jobId, int imported, int errors) {
        createIfAbsent(
                userId,
                "import-partial-" + jobId,
                "Імпорт частково успішний",
                String.format("Імпортовано %d рядків, помилок: %d", imported, errors),
                NotificationType.SYSTEM,
                NotificationSeverity.WARNING,
                "/imports",
                LocalDateTime.now().plusDays(30),
                NotificationPreferenceKey.IMPORT_PARTIAL
        );
    }

    @Transactional
    public void notifyImportProcessing(Long userId, Long jobId, String fileName) {
        createIfAbsent(
                userId,
                "import-processing-" + jobId,
                "Обробка CSV",
                "Файл обробляється: " + fileName,
                NotificationType.SYSTEM,
                NotificationSeverity.INFO,
                "/imports",
                LocalDateTime.now().plusDays(7),
                NotificationPreferenceKey.IMPORT_CSV_PROCESSING
        );
    }
}
