package com.flowiq.notifications.controller;

import com.flowiq.notifications.dto.MarkNotificationReadRequest;
import com.flowiq.notifications.dto.NotificationPageResponse;
import com.flowiq.notifications.dto.NotificationResponse;
import com.flowiq.notifications.dto.NotificationSummaryResponse;
import com.flowiq.notifications.entity.NotificationSeverity;
import com.flowiq.notifications.entity.NotificationType;
import com.flowiq.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<NotificationPageResponse> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationSeverity severity
    ) {
        return ResponseEntity.ok(notificationService.getNotifications(page, size, unreadOnly, type, severity));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount()));
    }

    @GetMapping("/summary")
    public ResponseEntity<NotificationSummaryResponse> getSummary() {
        return ResponseEntity.ok(notificationService.getSummary());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            @RequestBody(required = false) MarkNotificationReadRequest request
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(id));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead() {
        int updated = notificationService.markAllAsRead();
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
