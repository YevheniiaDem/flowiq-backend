package com.flowiq.notifications.dto;

import com.flowiq.notifications.entity.Notification;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private String title;
    private String message;
    private NotificationType type;
    private NotificationSeverity severity;
    private boolean read;
    private String actionUrl;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private LocalDateTime expiresAt;

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .severity(notification.getSeverity())
                .read(notification.isRead())
                .actionUrl(notification.getActionUrl())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .expiresAt(notification.getExpiresAt())
                .build();
    }
}
