# Notifications Module

**Package:** `com.flowiq.notifications`  
**API:** `/api/notifications/*`  
**Frontend:** `flowiq-frontend/src/features/notifications/`

## Components

| Class | Role |
|-------|------|
| `Notification` | Entity |
| `NotificationController` | REST |
| `NotificationService` | CRUD, read state |
| `NotificationRuleEngine` | Alert generation rules |
| `NotificationGeneratorService` | Idempotent create |
| `NotificationScheduler` | Daily 08:00 cron |

## Rule Engine Triggers

| Condition | Type | action_url |
|-----------|------|------------|
| FOP limit 70/85/95% | FOP_LIMIT | `/business-guide` |
| Tax deadline approaching | TAX | `/ai-accountant` |
| Revenue/expense anomaly | FINANCIAL | `/analytics` |
| Tax optimization insight | AI_INSIGHT | `/business-guide` |
| Import/report complete | REPORT/SYSTEM | module-specific |

## Deduplication

`uk_notifications_user_dedup` on `(user_id, deduplication_key)`.

## Frontend

- `NotificationCenterView` — full page `/notifications`
- `NotificationBell` — header dropdown
- `RecentNotificationsWidget` — dashboard

## API

[Notification API](../api/notification-api.md)

## TODO

- [ ] Wire EMAIL, TELEGRAM channels
- [ ] Push notification service
