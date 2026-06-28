# Notification Flow

**As-built:** 2026-06-28  
**Scope:** End-to-end notification lifecycle — generation, preference gating, persistence, UI delivery

> Preference settings detail: [NOTIFICATION_ARCHITECTURE.md](../NOTIFICATION_ARCHITECTURE.md)

## Overview

Notifications are **IN_APP only** in production. All writes pass through `NotificationGeneratorService.createIfAbsent()`, which checks `NotificationPreferenceService.isInAppEnabled()` before inserting.

## Notification Model

| Attribute | Values |
|-----------|--------|
| **Types** | `TAX`, `FOP_LIMIT`, `FINANCIAL`, `AI_INSIGHT`, `REPORT`, `SYSTEM` |
| **Severities** | `INFO`, `SUCCESS`, `WARNING`, `CRITICAL` |
| **Channel (stored)** | Always `IN_APP` today |
| **Deduplication** | Unique `(user_id, deduplication_key)` |

## Generation Sources

```mermaid
flowchart TB
    subgraph Scheduled["Scheduled (08:00 daily)"]
        NSCH[NotificationScheduler]
        NRE[NotificationRuleEngine]
    end

    subgraph OnDemand["Event-driven"]
        IMP[ImportService]
        REP[ReportsService]
        TRE[TaskRuleEngine]
    end

    subgraph Gate["Single write gate"]
        NGS[NotificationGeneratorService]
        NPS[NotificationPreferenceService]
    end

    subgraph Store["Persistence"]
        NR[NotificationRepository]
        DB[(notifications)]
    end

    NSCH --> NRE --> NGS
    IMP --> NGS
    REP --> NGS
    TRE --> NGS
    NGS --> NPS
    NPS -->|IN_APP enabled| NR --> DB
    NPS -->|disabled| SKIP[Skip — no row]
```

## Daily Rule Engine Flow

```mermaid
sequenceDiagram
    participant SCH as NotificationScheduler
    participant UR as UserRepository
    participant NRE as NotificationRuleEngine
    participant TR as TransactionRepository
    participant FPS as FopProfileService
    participant NGS as NotificationGeneratorService
    participant NPS as NotificationPreferenceService
    participant DB as PostgreSQL

    Note over SCH: Cron 0 0 8 * * * (08:00)
    SCH->>UR: findAllByActiveTrue()
    loop each active user
        SCH->>NRE: generateForUser(userId)
        NRE->>TR: load YTD revenue/expenses
        NRE->>FPS: get FOP group + limits
        NRE->>NRE: Evaluate rules
        Note over NRE: FOP limit 70/85/95%<br/>Tax deadlines May/Aug/Nov/Feb<br/>Expense spike, revenue drop<br/>Negative cash flow
        NRE->>NGS: createIfAbsent(userId, key, type, severity, ...)
        NGS->>NPS: isInAppEnabled(userId, preferenceKey)
        alt enabled
            NGS->>DB: INSERT notifications (dedup key)
        else disabled
            NGS-->>NRE: skip
        end
    end
```

## Event-Driven Notifications

```mermaid
sequenceDiagram
    participant SVC as ImportService / ReportsService
    participant NGS as NotificationGeneratorService
    participant TGS as TaskGeneratorService
    participant DB as PostgreSQL
    participant UI as Frontend NotificationsView

    alt Import completed
        SVC->>NGS: notifyImportCompleted(userId, jobId, count)
        SVC->>TGS: createImportReviewTask(...)
    else Import failed / partial
        SVC->>NGS: notifyImportFailed / notifyImportPartial
    else Report completed
        SVC->>NGS: notifyReportCompleted(...)
        SVC->>TGS: createReportReviewTask(...)
    else Report failed
        SVC->>NGS: notifyReportFailed(...)
    end
    NGS->>DB: INSERT notifications (if preference allows)

    UI->>DB: GET /api/notifications (poll on page load)
    UI->>DB: PUT /api/notifications/{id}/read
```

## User Read Flow (Frontend)

```mermaid
sequenceDiagram
    participant Bell as NotificationBell
    participant View as NotificationsView
    participant API as /api/notifications
    participant DB as PostgreSQL

    Bell->>API: GET /unread-count
    API->>DB: COUNT WHERE is_read=false
    Bell-->>Bell: Badge count

    View->>API: GET /notifications?page&size&unreadOnly
    API->>DB: SELECT paginated
    View->>API: PUT /{id}/read or /read-all
    View->>View: Navigate via action_url
```

### Deep link routes (`action_url`)

| URL | Trigger |
|-----|---------|
| `/imports` | Import lifecycle |
| `/business-guide` | FOP limit warnings |
| `/ai-accountant` | Tax deadlines |
| `/analytics` | Revenue/expense anomalies |
| `/tasks` | Task reminders |
| `/reports` | Report ready |

## Preference Gate (Summary)

```mermaid
flowchart LR
    A[Notification event] --> B{NotificationPreferenceKey<br/>IN_APP enabled?}
    B -->|Yes| C[INSERT notifications]
    B -->|No| D[Silent skip]
    C --> E[Unread count + UI feed]
```

24 preference keys in 5 categories — see [NOTIFICATION_ARCHITECTURE.md](../NOTIFICATION_ARCHITECTURE.md).

## Future: Multi-Channel Delivery

EMAIL, PUSH, TELEGRAM channels exist in schema and preferences UI (disabled / "Soon") but **no outbound dispatchers** are implemented.

```mermaid
flowchart LR
    NGS[NotificationGeneratorService] --> INAPP[IN_APP ✅]
    NGS -.-> EMAIL[EMAIL ❌]
    NGS -.-> PUSH[PUSH ❌]
    NGS -.-> TG[TELEGRAM ❌]
```

## Related

- [NOTIFICATION_ARCHITECTURE.md](../NOTIFICATION_ARCHITECTURE.md)
- [Notification API](../api/notification-api.md)
- [SRS §13](../product/SRS.md)
