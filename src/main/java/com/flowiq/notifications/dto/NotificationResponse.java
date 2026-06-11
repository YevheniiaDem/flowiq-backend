package com.flowiq.notifications.dto;

import com.flowiq.notifications.entity.Notification;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Notification item")
public class NotificationResponse {

    @Schema(description = "Notification ID", example = "1")
    private Long id;

    @Schema(description = "Notification title", example = "Нагадування про податок")
    private String title;

    @Schema(description = "Notification message body")
    private String message;

    @Schema(description = "Notification category", example = "TAX")
    private NotificationType type;

    @Schema(description = "Severity level", example = "WARNING")
    private NotificationSeverity severity;

    @Schema(description = "Whether the notification has been read", example = "false")
    private boolean read;

    @Schema(description = "Deep link URL for navigation", example = "/analytics")
    private String actionUrl;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when marked as read")
    private LocalDateTime readAt;

    @Schema(description = "Expiration timestamp")
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
