# Notification API

**Controller:** `com.flowiq.notifications.controller.NotificationController`  
**Base path:** `/api/notifications`  
**Auth:** JWT required

## Endpoints

| Method | Path | Params/Body | Response |
|--------|------|-------------|----------|
| GET | `/` | `page`, `size`, `unreadOnly`, `type`, `severity` | `NotificationPageResponse` |
| GET | `/unread-count` | — | `{ "count": 5 }` |
| GET | `/summary` | — | `NotificationSummaryResponse` |
| PUT | `/{id}/read` | Optional `MarkNotificationReadRequest` | `NotificationResponse` |
| PUT | `/read-all` | — | `{ "updated": 12 }` |
| DELETE | `/{id}` | — | 204 |

## Enums

**NotificationType:** `TAX`, `FOP_LIMIT`, `FINANCIAL`, `AI_INSIGHT`, `REPORT`, `SYSTEM`  
**NotificationSeverity:** `INFO`, `SUCCESS`, `WARNING`, `CRITICAL`  
**NotificationChannel:** `IN_APP` (used), `EMAIL`, `PUSH`, `TELEGRAM` (not wired)

## Example: Notification Item

```json
{
  "id": 42,
  "title": "FOP income limit at 85%",
  "message": "You have used 85% of your annual Group 2 income limit.",
  "type": "FOP_LIMIT",
  "severity": "WARNING",
  "isRead": false,
  "actionUrl": "/business-guide",
  "createdAt": "2026-06-11T08:00:00"
}
```

## Generation

Daily at 08:00 via `NotificationScheduler` → `NotificationRuleEngine.generateForUser()`.

Deduplication via `deduplication_key` (unique per user).

## Related

- [Notifications Module](../modules/notifications.md)
