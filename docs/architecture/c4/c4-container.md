# C4 Model — Level 2: Container Diagram

**As-built:** 2026-06-17  
**Repositories:** `flowiq-frontend` (Next.js 16), `flowiq-backend` (Spring Boot 3.5)

## Container Diagram

```mermaid
C4Container
    title FlowIQ — Container Diagram (As-Built)

    Person(user, "User", "FOP entrepreneur")

    Container_Boundary(flowiq, "FlowIQ Platform") {
        Container(frontend, "FlowIQ Frontend", "Next.js 16, React 19, TypeScript", "SPA: dashboard, transactions, imports, analytics, forecasts, reports, AI Accountant, chat, business guide")
        Container(backend, "FlowIQ Backend", "Spring Boot 3.5, Java 17", "REST API, JWT auth, business logic, schedulers, rule engines")
        ContainerDb(db, "PostgreSQL 15", "Relational DB", "users, transactions, tasks, notifications, knowledge_articles, import_jobs, report_jobs, chat_*")
    }

    Container_Boundary(intelligence, "Intelligence Layer (inside Backend JVM)") {
        Container(scheduler, "Scheduler Layer", "Spring @Scheduled", "Daily task generation 07:30, notification rules 08:00")
        Container(ai, "AI Layer", "Rule engines + provider interfaces", "Recommendations, forecasts, categorization, knowledge assist — deterministic rules today")
        Container(reporting, "Reporting Layer", "ReportsService + PDF/Excel/CSV renderers", "Financial report generation and download")
        Container(import, "Import Layer", "ImportService + CSV strategies", "Bank CSV parse, categorization, transaction persist")
    }

    System_Ext(csv, "CSV Files", "User bank exports")
    System_Ext(banks, "Bank APIs", "Planned")
    System_Ext(llm, "AI Providers", "Planned LLM backends")

    Rel(user, frontend, "Uses", "HTTPS")
    Rel(frontend, backend, "API calls", "JSON / REST / JWT")
    Rel(backend, db, "Reads/writes", "JDBC, JPA, Flyway")
    Rel(user, csv, "Uploads")
    Rel(csv, import, "POST /api/imports/upload")
    Rel(import, db, "Persists transactions, import_jobs")
    Rel(scheduler, db, "Reads transactions, writes tasks/notifications")
    Rel(ai, db, "Reads transactions, knowledge_articles")
    Rel(reporting, db, "Reads transactions, writes report_jobs")
    Rel(backend, banks, "Future", "Not implemented")
    Rel(ai, llm, "Future", "Provider beans — none active")
```

## Container Descriptions

### FlowIQ Frontend

| Attribute | Value |
|-----------|-------|
| **Technology** | Next.js 16, React 19, TypeScript, Tailwind CSS |
| **Location** | `flowiq-frontend/` |
| **Deployment** | Vercel (`flowiq.vercel.app` in backend CORS) or Docker (`Dockerfile` with `output: standalone`) |
| **Responsibilities** | UI routing, auth token storage, API client (`src/services/api.ts`), i18n (en/uk), preferences (language/currency/theme in `localStorage`) |
| **API base URL** | `NEXT_PUBLIC_API_URL` or `http://localhost:8080/api` |

**Hybrid data:** Most modules call backend API. Business Guide profile/groups/taxes/KVED and tax profile card use **frontend static mock data**. See [Data Sources](../data-sources.md).

### FlowIQ Backend

| Attribute | Value |
|-----------|-------|
| **Technology** | Spring Boot 3.5.14, Java 17, Spring Security, Spring Data JPA |
| **Location** | `flowiq-backend/` |
| **Deployment** | JAR or Docker (`Dockerfile` multi-stage, healthcheck `/api/health`) |
| **Responsibilities** | REST controllers, services, JWT auth, Flyway migrations, OpenAPI/Swagger |

**Package layout:** `controller/`, `service/`, `entity/`, `repository/`, plus domain packages: `forecasts/`, `knowledge/`, `notifications/`, `tasks/`, `categorization/`, `aiaccountant/`, `reports/`.

### PostgreSQL

| Attribute | Value |
|-----------|-------|
| **Version** | 15 (Alpine in `compose.yaml`) |
| **Migrations** | Flyway V1–V5 in `src/main/resources/db/migration/` |
| **Tables** | `users`, `transactions`, `chat_conversations`, `chat_messages`, `import_jobs`, `report_jobs`, `notifications`, `tasks`, `knowledge_articles` |

Local dev: `compose.yaml` or Spring Docker Compose (`spring.docker.compose.enabled=true`).

### Scheduler Layer

| Component | Schedule | Responsibility |
|-----------|----------|----------------|
| `DailyTaskScheduler` | `0 30 7 * * *` (07:30 daily) | Iterates active users → `TaskRuleEngine.generateForUser()` |
| `NotificationScheduler` | `0 0 8 * * *` (08:00 daily) | Iterates active users → `NotificationRuleEngine.generateForUser()` |

On-demand generation also runs when user opens Tasks (`TaskService.ensureGeneratedTasks`).

### AI Layer

Not a separate deployable container — runs inside the backend JVM.

| Component type | Classes | Role |
|----------------|---------|------|
| Rule engines | `AIRecommendationEngine`, `ForecastEngine`, `RuleBasedForecastProvider`, `DatabaseKnowledgeProvider`, `CategorizationEngine`, `NotificationRuleEngine`, `TaskRuleEngine` | Deterministic FOP/tax/financial logic |
| Inline intelligence | `DashboardService`, `ChatService`, `AnalyticsService` | Health scores, insights, template chat replies |
| Provider interfaces | `AIInsightProvider`, `ForecastProvider`, `KnowledgeProvider`, `AnalyticsInsightProvider`, `CategorizationProvider` | Extension points for future LLM — **no external implementations** |
| Data prep | `TransactionInsightService` | Builds analysis context (future AI hook) |

See [AI Quality Factory](../ai-quality-factory.md) and [AI Agents Architecture](../ai-agents-architecture.md).

### Reporting Layer

| Component | Role |
|-----------|------|
| `ReportsController` | REST: list, preview, generate, download |
| `ReportsService` | Aggregates from `AnalyticsService` + transactions, persists `ReportJob` |
| `ReportFileGenerator` | Routes to PDF (`OpenPdfReportRenderer`), Excel (`PoiReportRenderer`), CSV |
| **Report types** | `PROFIT_AND_LOSS`, `CASH_FLOW`, `REVENUE_SUMMARY`, `EXPENSE_SUMMARY`, `TAX_SUMMARY`, `FOP_SUMMARY` |

### Import Layer

| Component | Role |
|-----------|------|
| `ImportController` | `POST /api/imports/upload`, list jobs |
| `ImportService` | Orchestrates parse → categorize → persist |
| `CsvImportStrategyResolver` | Selects Monobank / PrivatBank / Universal strategy |
| `CategorizationEngine` | Keyword rules (`DefaultCategoryRules`) + optional `CategorizationProvider` |

## Communication Patterns

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant B as Backend API
    participant I as Import Layer
    participant A as AI Layer
    participant S as Scheduler
    participant DB as PostgreSQL

    U->>F: Login
    F->>B: POST /api/auth/login
    B->>DB: Validate user
    B-->>F: JWT

    U->>F: Upload CSV
    F->>B: POST /api/imports/upload
    B->>I: Parse + categorize
    I->>DB: Save transactions

    U->>F: Open Dashboard
    F->>B: GET /api/dashboard/*
    B->>A: Rule-based insights
    B->>DB: Read/write (seed if empty)

    Note over S,DB: 07:30 tasks, 08:00 notifications
    S->>DB: Rule engines → tasks, notifications
```

## Related

- [Context Diagram](c4-context.md)
- [Backend Architecture](../backend-architecture.md)
- [Frontend Architecture](../frontend-architecture.md)
- [Data Sources](../data-sources.md)
