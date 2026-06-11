package com.flowiq.notifications.controller;

import com.flowiq.config.OpenApiConfig;
import com.flowiq.config.openapi.ApiErrorResponses;
import com.flowiq.notifications.dto.MarkNotificationReadRequest;
import com.flowiq.notifications.dto.NotificationPageResponse;
import com.flowiq.notifications.dto.NotificationResponse;
import com.flowiq.notifications.dto.NotificationSummaryResponse;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Notifications", description = "In-app notification center for tax alerts, financial insights, and system events")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "List notifications",
            description = "Returns a paginated list of notifications for the authenticated user with optional filters."
    )
    @ApiResponse(responseCode = "200", description = "Paginated notification list",
            content = @Content(schema = @Schema(implementation = NotificationPageResponse.class)))
    @ApiErrorResponses
    @GetMapping
    public ResponseEntity<NotificationPageResponse> getNotifications(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Show only unread notifications") @RequestParam(required = false) Boolean unreadOnly,
            @Parameter(description = "Filter by notification type") @RequestParam(required = false) NotificationType type,
            @Parameter(description = "Filter by severity") @RequestParam(required = false) NotificationSeverity severity
    ) {
        return ResponseEntity.ok(notificationService.getNotifications(page, size, unreadOnly, type, severity));
    }

    @Operation(summary = "Unread count", description = "Returns the number of unread notifications for the badge indicator.")
    @ApiResponse(responseCode = "200", description = "Unread notification count")
    @ApiErrorResponses
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount()));
    }

    @Operation(summary = "Notification summary", description = "Returns aggregated notification statistics for the notification center dashboard.")
    @ApiResponse(responseCode = "200", description = "Notification summary",
            content = @Content(schema = @Schema(implementation = NotificationSummaryResponse.class)))
    @ApiErrorResponses
    @GetMapping("/summary")
    public ResponseEntity<NotificationSummaryResponse> getSummary() {
        return ResponseEntity.ok(notificationService.getSummary());
    }

    @Operation(summary = "Mark notification as read", description = "Marks a single notification as read by ID.")
    @ApiResponse(responseCode = "200", description = "Updated notification",
            content = @Content(schema = @Schema(implementation = NotificationResponse.class)))
    @ApiErrorResponses
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @Parameter(description = "Notification ID") @PathVariable Long id,
            @RequestBody(required = false) MarkNotificationReadRequest request
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @Operation(summary = "Mark all as read", description = "Marks all unread notifications as read for the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Number of notifications updated")
    @ApiErrorResponses
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead() {
        int updated = notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @Operation(summary = "Delete notification", description = "Permanently deletes a notification by ID.")
    @ApiResponse(responseCode = "204", description = "Notification deleted")
    @ApiErrorResponses
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Notification ID") @PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
