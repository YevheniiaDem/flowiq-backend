# System Overview

## High-Level Architecture

```mermaid
flowchart TB
    subgraph Client["Browser (Next.js 16)"]
        UI[React App Router]
        CTX[PreferencesContext]
        API_CLIENT[Axios apiClient]
    end

    subgraph Backend["Spring Boot 3.5 API"]
        SEC[Security / JWT Filter]
        CTRL[REST Controllers]
        SVC[Services]
        ENG[Rule Engines]
        PROV[AI Provider Interfaces]
        REPO[JPA Repositories]
        SCH[Schedulers]
    end

    subgraph Data["PostgreSQL 15"]
        DB[(flowiq database)]
        FLY[Flyway Migrations]
    end

    UI --> API_CLIENT
    API_CLIENT -->|HTTPS + Bearer JWT| SEC
    SEC --> CTRL
    CTRL --> SVC
    SVC --> ENG
    SVC --> PROV
    SVC --> REPO
    SCH --> SVC
    REPO --> DB
    FLY --> DB
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
| Migrations | Flyway V1–V5 |
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

```mermaid
flowchart LR
    DEV[Developer Machine]
    FE[next dev :3000]
    BE[spring-boot :8080]
    PG[(postgres :5432)]

    DEV --> FE
    FE -->|REST| BE
    BE --> PG
```

Production target: frontend on Vercel (`https://flowiq.vercel.app` in CORS); backend via `Dockerfile` (manual build/deploy) or managed JVM host — **CD not automated**. See [Docker](../deployment/docker.md) and [CI/CD](../deployment/ci-cd.md).

## Cross-References

- [Backend Architecture](backend-architecture.md)
- [Frontend Architecture](frontend-architecture.md)
- [Database Architecture](database-architecture.md)
- [AI Architecture](ai-architecture.md)
- [Local Setup](../deployment/local-setup.md)
