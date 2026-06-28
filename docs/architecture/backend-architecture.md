# Backend Architecture

**As-built:** 2026-06-28  
**Entry point:** `com.flowiq.FlowiqBackendApplication`  
**Base API path:** `/api`  
**Source root:** `src/main/java/com/flowiq/`

> Flow diagrams: [flows/](flows/) · Dependencies: [module-dependencies.md](module-dependencies.md) · C4: [c4/c4-component.md](c4/c4-component.md)

## Layered Structure

```mermaid
flowchart TB
    subgraph Presentation
        CTRL[Controllers]
        DTO[DTOs request/response]
    end
    subgraph Application
        SVC[Services]
        RULE[Rule Engines]
        SCHED[Schedulers]
    end
    subgraph Domain
        ENT[Entities]
        ENUM[Enums]
    end
    subgraph Infrastructure
        REPO[Repositories]
        SEC[Security JWT]
        CFG[Config]
    end

    CTRL --> SVC
    CTRL --> DTO
    SVC --> RULE
    SVC --> REPO
    SVC --> ENT
    SCHED --> SVC
    REPO --> ENT
```

## Package Layout

### Core (`com.flowiq`)

| Package | Responsibility |
|---------|----------------|
| `controller` | Auth, Health, Transactions, Imports, Dashboard, Analytics, AI Accountant, Chat, Reports |
| `profile` | Profile, FOP, avatar, sessions, password |
| `audit` | `@Auditable`, async audit log writer |
| `service` | Core business services |
| `entity` | User, Transaction, Chat, ImportJob, ReportJob |
| `repository` | Spring Data JPA for core entities |
| `dto.request` / `dto.response` | API contracts with `@Schema` |
| `config` | Security, CORS, OpenAPI, `AppPreferencesFilter` |
| `security` | JWT, `UserPrincipal`, filter chain |
| `exception` | `GlobalExceptionHandler`, `ErrorResponse` |
| `util` | `CurrencyFormatter`, `TransactionDateValidator` |

### Feature Modules

| Package | Controllers | Persistence |
|---------|-------------|-------------|
| `forecasts` | `ForecastController` | Stateless (reads `TransactionRepository`) |
| `tasks` | `TaskController` | `tasks` table |
| `notifications` | `NotificationController`, `NotificationPreferenceController` | `notifications`, `notification_preferences` |
| `knowledge` | `BusinessGuideController` | `knowledge_articles` table |

### Supporting

| Package | Role |
|---------|------|
| `aiaccountant` | `AIRecommendationEngine`, `AIInsightProvider` |
| `analytics` | `AnalyticsInsightProvider` |
| `categorization` | `CategorizationEngine`, `DefaultCategoryRules` |
| `importcsv` | Bank CSV parsers |
| `reports` | PDF (`OpenPdfReportRenderer`), Excel (`PoiReportRenderer`) |

## Controllers (15 total, ~90 endpoints)

| Controller | Base path |
|------------|-----------|
| Auth, Health, Transaction, Import, Dashboard, Analytics | `/api/...` |
| Forecast, AIAccountant, Chat, Task, Notification, Reports, BusinessGuide | `/api/...` |
| Profile | `/api/profile` |
| NotificationPreference | `/api/settings/notifications` |

See [OpenAPI Overview](../api/openapi-overview.md) and [REQUEST_FLOW_MAP.md](REQUEST_FLOW_MAP.md).

## Services — Key Interactions

```mermaid
flowchart LR
    DS[DashboardService] --> TR[TransactionRepository]
    DS --> TIS[TransactionInsightService]
    FS[ForecastService] --> FE[ForecastEngine]
    FS --> TR
    TS[TaskService] --> TaskRepo[TaskRepository]
    TRE[TaskRuleEngine] --> TS
    TRE --> NRE[NotificationRuleEngine]
    KS[KnowledgeService] --> KRepo[KnowledgeArticleRepository]
    KS --> KP[KnowledgeProvider]
    IS[ImportService] --> CE[CategorizationEngine]
    IS --> TGS[TaskGeneratorService]
```

## Schedulers

| Class | Cron | Action |
|-------|------|--------|
| `NotificationScheduler` | `0 0 8 * * *` | `NotificationRuleEngine.generateForUser` for all active users |
| `DailyTaskScheduler` | `0 30 7 * * *` | `TaskRuleEngine.generateForUser` for all active users |

> **Note:** Class renamed from `TaskScheduler` to `DailyTaskScheduler` to avoid Spring Boot `taskScheduler` bean conflict.

## Configuration

| File | Purpose |
|------|---------|
| `application.properties` | DB, JWT, Flyway, upload limits, OpenAPI |
| `compose.yaml` | PostgreSQL 15 for local dev |

## Exception Handling

`GlobalExceptionHandler` (`@RestControllerAdvice`):

| Exception | HTTP |
|-----------|------|
| `MethodArgumentNotValidException` | 400 + field errors |
| `BadRequestException` | 400 |
| `UnauthorizedException`, `BadCredentialsException` | 401 |
| `ResourceNotFoundException` | 404 |
| `Exception` | 500 |

## Seed Data

| Component | Trigger | Data |
|-----------|---------|------|
| `DemoUserSeedService` | `ApplicationRunner` | `demo@flowiq.ai` |
| `TransactionSeedService` | On empty DB | 6 months demo transactions |
| Flyway V5 | Migration | 20 knowledge articles |
| Flyway V6–V8 | Migration | `audit_log`, profile/sessions, notification preferences |

## Related Documents

- [Frontend Architecture](frontend-architecture.md)
- [Database Architecture](database-architecture.md)
- [Database ER Diagram](database-er-diagram.md)
- Module docs in [modules/](../modules/)
