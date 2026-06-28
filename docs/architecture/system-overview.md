# System Overview

**As-built:** 2026-06-28  
**Index:** [README.md](README.md) — full architecture documentation map

## High-Level Architecture

Three repositories form the FlowIQ platform:

```mermaid
flowchart TB
    subgraph Client["flowiq-frontend — Next.js 16"]
        UI[React App Router]
        CTX[PreferencesContext]
        API_CLIENT[Axios apiClient]
    end

    subgraph Backend["flowiq-backend — Spring Boot 3.5"]
        SEC[Security / JWT Filter]
        CTRL[15 REST Controllers]
        SVC[Services + Rule Engines]
        PROV[AI Provider Interfaces]
        REPO[JPA Repositories]
        SCH[Schedulers 07:30 / 08:00]
        AUD[Audit Async Writer]
    end

    subgraph Data["PostgreSQL 15"]
        DB[(13 tables Flyway V1–V8)]
    end

    subgraph QA["flowiq-automation"]
        TEST[TestNG + Rest Assured + Playwright]
    end

    UI --> API_CLIENT
    API_CLIENT -->|HTTPS + Bearer JWT| SEC
    SEC --> CTRL --> SVC
    SVC --> ENG[Rule Engines] & PROV & REPO & AUD
    SCH --> SVC
    REPO --> DB
    TEST --> Backend & Client
```

## Request Flow

```mermaid
sequenceDiagram
    participant B as Browser
    participant F as JwtAuthenticationFilter
    participant C as Controller
    participant S as Service
    participant R as Repository
    participant D as PostgreSQL

    B->>F: HTTP + Authorization Bearer
    F->>F: Validate JWT access token
    F->>C: Authenticated request
    C->>S: Business logic
    S->>R: JPA query
    R->>D: SQL
    D-->>R: Rows
    R-->>S: Entity/DTO
    S-->>C: Response DTO
    C-->>B: JSON 200
```

## Technology Stack

| Layer | Technology |
|-------|------------|
| Frontend | Next.js 16, React 19, TypeScript, Tailwind 4, shadcn/ui, Recharts, Axios |
| Backend | Spring Boot 3.5.14, Java 17, Spring Security, Spring Data JPA |
| Database | PostgreSQL 15 (Docker Compose) |
| Migrations | Flyway V1–V8 |
| Automation | TestNG, Rest Assured, Playwright Java (`flowiq-automation`) |
| API Docs | springdoc-openapi 2.8 |
| PDF Reports | OpenPDF |
| Excel Reports | Apache POI |

## Module Map

| Domain | Backend Package | Frontend Feature |
|--------|-----------------|------------------|
| Auth | `controller`, `security` | `features/auth` |
| Transactions | `controller`, `entity` | `features/transactions` |
| Dashboard | `controller`, `service` | `features/dashboard` |
| Forecasts | `forecasts.*` | `features/forecasts` |
| Tasks | `tasks.*` | `features/tasks` |
| Notifications | `notifications.*` | `features/notifications` |
| Knowledge | `knowledge.*` | `features/business-guide` |
| Analytics | `controller`, `service` | `features/analytics` |
| Reports | `reports.*`, `controller` | `features/reports` |
| AI Accountant | `aiaccountant.*` | `features/ai-accountant` |
| Chat | `controller`, `service` | `features/chat` |
| Imports | `importcsv.*`, `service` | `features/imports` |

## Deployment Topology (Current)

See [deployment-architecture.md](deployment-architecture.md) for full diagram.

```mermaid
flowchart LR
    DEV[Developer / CI]
    FE[next dev or Vercel :3000]
    BE[spring-boot or container :8080]
    PG[(postgres :5432)]
    AUTO[flowiq-automation CI]

    DEV --> FE & BE & AUTO
    FE -->|REST /api| BE
    BE --> PG
    AUTO --> FE & BE & PG
```

Production: frontend on Vercel (`https://flowiq.vercel.app`); backend JAR/Docker — **CD not automated**. See [cicd-architecture.md](cicd-architecture.md).

## Process Flows

| Flow | Document |
|------|----------|
| Authentication | [flows/authentication-flow.md](flows/authentication-flow.md) |
| Notifications | [flows/notification-flow.md](flows/notification-flow.md) |
| CSV import | [flows/import-flow.md](flows/import-flow.md) |
| Forecasts | [flows/forecast-flow.md](flows/forecast-flow.md) |
| AI / rules | [flows/ai-flow.md](flows/ai-flow.md) |
| Reports | [flows/reporting-flow.md](flows/reporting-flow.md) |

## Cross-References

- [C4 Context](c4/c4-context.md) · [C4 Container](c4/c4-container.md) · [C4 Component](c4/c4-component.md)
- [Module Dependencies](module-dependencies.md)
- [Backend](backend-architecture.md) · [Frontend](frontend-architecture.md) · [Automation](automation-architecture.md)
- [Database ER](database-er-diagram.md) · [Test Architecture](test-architecture.md)
- [SRS](../product/SRS.md)
