package com.flowiq.unit.notifications;

import com.flowiq.notifications.entity.Notification;
import com.flowiq.notifications.entity.NotificationChannel;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.preferences.NotificationPreferenceKey;
import com.flowiq.notifications.preferences.NotificationPreferenceService;
import com.flowiq.notifications.repository.NotificationRepository;
import com.flowiq.notifications.service.NotificationGeneratorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationGeneratorService unit tests")
class NotificationGeneratorServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationPreferenceService preferenceService;

    @InjectMocks
    private NotificationGeneratorService notificationGeneratorService;

    @Test
    @DisplayName("createIfAbsent saves notification when enabled and absent")
    void createIfAbsent_success() {
        when(preferenceService.isInAppEnabled(1L, NotificationPreferenceKey.REPORT_READY)).thenReturn(true);
        when(notificationRepository.existsByUserIdAndDeduplicationKey(1L, "dedup-1")).thenReturn(false);

        notificationGeneratorService.createIfAbsent(
                1L, "dedup-1", "Title", "Message",
                NotificationType.REPORT, NotificationSeverity.SUCCESS,
                "/reports", null, NotificationPreferenceKey.REPORT_READY
        );

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(captor.getValue().isRead()).isFalse();
    }

    @Test
    @DisplayName("createIfAbsent skips when preference disabled")
    void createIfAbsent_preferenceDisabled() {
        when(preferenceService.isInAppEnabled(1L, NotificationPreferenceKey.REPORT_READY)).thenReturn(false);

        notificationGeneratorService.createIfAbsent(
                1L, "dedup-1", "Title", "Message",
                NotificationType.REPORT, NotificationSeverity.SUCCESS,
                "/reports", null, NotificationPreferenceKey.REPORT_READY
        );

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("notifyReportCompleted creates PDF-specific notification")
    void notifyReportCompleted_pdf() {
        when(preferenceService.isInAppEnabled(eq(1L), any())).thenReturn(true);
        when(notificationRepository.existsByUserIdAndDeduplicationKey(eq(1L), any())).thenReturn(false);

        notificationGeneratorService.notifyReportCompleted(1L, 5L, "report.pdf", "PDF");

        verify(notificationRepository, org.mockito.Mockito.times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("notifyImportFailed creates critical notification")
    void notifyImportFailed() {
        when(preferenceService.isInAppEnabled(1L, NotificationPreferenceKey.IMPORT_FAILED)).thenReturn(true);
        when(notificationRepository.existsByUserIdAndDeduplicationKey(1L, "import-failed-3")).thenReturn(false);

        notificationGeneratorService.notifyImportFailed(1L, 3L, "bad.csv");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(NotificationSeverity.CRITICAL);
    }
}
