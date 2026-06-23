# Flowiq Backend - AI Business Operator

Spring Boot backend for FlowIQ — a Ukrainian FOP financial management platform with rule-based AI insights, forecasts, tasks, and notifications.

**Status:** MVP functional  
**Version:** 0.0.1-SNAPSHOT  
**Last Updated:** June 17, 2026

## Tech Stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 17 |
| Framework | Spring Boot 3.5.14 |
| Database | PostgreSQL 15 + Flyway migrations |
| Security | Spring Security + JWT (access + refresh tokens) |
| ORM | Spring Data JPA (Hibernate, `ddl-auto=validate`) |
| API docs | springdoc-openapi (Swagger UI) |
| Build | Maven + JaCoCo |

## Project Structure

```
src/main/java/com/flowiq/
├── config/              # Security, CORS, feature flags
├── controller/          # REST controllers
├── service/             # Core business logic
├── repository/          # JPA repositories
├── entity/              # JPA entities
├── dto/                 # Request/response DTOs
├── security/            # JWT filter, UserPrincipal
├── forecasts/           # Forecast engine + providers
├── knowledge/           # Business Guide articles
├── notifications/       # Notification rules + scheduler
├── tasks/               # Task rules + scheduler
├── categorization/      # CSV import categorization
├── aiaccountant/        # Recommendation engine + provider interface
└── reports/             # PDF/Excel/CSV generation

src/main/resources/
├── application.properties
├── application-docker.properties
└── db/migration/        # Flyway V1–V5
```

## Setup & Run

### Prerequisites

- Java 17+
- Maven 3.8+ (or use `./mvnw`)
- PostgreSQL 15+ (or Docker Compose)

### Local Development

```bash
git clone https://github.com/YevheniiaDem/flowiq-backend.git
cd flowiq-backend

# Start PostgreSQL (or let Spring Docker Compose do it)
docker compose up -d

# Run application (Flyway migrates on startup)
./mvnw spring-boot:run
```

Demo user seeded on startup: `demo@flowiq.ai` / `demo123`

### Docker

```bash
docker build -t flowiq-backend .
docker run -p 8080:8080 flowiq-backend
```

See [docs/deployment/docker.md](docs/deployment/docker.md).

## API Endpoints (Implemented)

### Authentication
- `POST /api/auth/register` — User registration
- `POST /api/auth/login` — Login, returns JWT
- `POST /api/auth/logout` — Logout
- `POST /api/auth/refresh` — Refresh JWT token
- `GET /api/auth/me` — Current user

### Dashboard
- `GET /api/dashboard/stats` — Statistics
- `GET /api/dashboard/health` — Business health score
- `GET /api/dashboard/insights` — Rule-based insights
- `GET /api/dashboard/summary` — AI summary

### Transactions
- `GET /api/transactions` — Paginated list
- `POST /api/transactions` — Create
- `PUT /api/transactions/{id}` — Update
- `DELETE /api/transactions/{id}` — Delete

### Analytics
- `GET /api/analytics/overview` — Overview metrics
- `GET /api/analytics/fop-insights` — FOP group, limits, taxes

### Forecasts
- `GET /api/forecasts/revenue`, `/expenses`, `/profit`, `/taxes`, `/fop-limit`, `/summary`

### AI Accountant
- `GET /api/ai-accountant/health`, `/recommendations`, `/tax-advisor`, `/forecasts`
- `POST /api/ai-accountant/chat`

### Imports
- `POST /api/imports/upload` — Upload bank CSV
- `GET /api/imports` — List import jobs

### Reports
- `GET /api/reports` — List report jobs
- `POST /api/reports/preview`, `/generate`
- `GET /api/reports/{id}/download`

### Other modules
- `/api/tasks/*` — Tasks & deadlines
- `/api/notifications/*` — Notifications
- `/api/business-guide/*` — Knowledge base
- `/api/chat/*` — Chat conversations

### Bank Integrations (planned — not implemented)

Feature flag: `flowiq.features.bank-integrations-enabled=false`  
See [Bank Integrations Roadmap](docs/roadmap/BANK_INTEGRATIONS_ROADMAP.md).

## Security

- **JWT** access token (24h) + refresh token (7d) — implemented
- BCrypt password hashing
- CORS: `localhost:3000`, `https://flowiq.vercel.app`
- Roles: `ADMIN`, `USER`, `VIEWER` (role enforcement partially documented — see `docs/security/`)

## API Documentation

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs

## Database

PostgreSQL with Flyway migrations (V1–V5). Hibernate does not auto-modify schema.

**Tables:** `users`, `transactions`, `chat_conversations`, `chat_messages`, `import_jobs`, `report_jobs`, `notifications`, `tasks`, `knowledge_articles`

```bash
./mvnw flyway:migrate   # requires Flyway Maven plugin or app startup
```

## Testing

```bash
./mvnw test                  # 9 test classes, unit + renderer tests
./mvnw test jacoco:report    # coverage report
```

See [docs/UNIT-TEST-COVERAGE.md](docs/UNIT-TEST-COVERAGE.md) and [docs/qa/test-strategy.md](docs/qa/test-strategy.md).

**Note:** No integration tests or frontend tests yet. Docker build uses `-DskipTests`.

## Documentation

Full documentation hub: [docs/index.md](docs/index.md)

Key architecture docs:
- [C4 Context](docs/architecture/c4/c4-context.md)
- [C4 Containers](docs/architecture/c4/c4-container.md)
- [Data Sources](docs/architecture/data-sources.md)
- [AI Quality Factory](docs/architecture/ai-quality-factory.md)
- [Architecture Review Readiness](docs/architecture/ARCHITECTURE_REVIEW_READINESS.md)

## Development Status

### Implemented
- PostgreSQL + Flyway migrations (V1–V5)
- JWT authentication (login, register, refresh, me)
- REST API for all MVP modules
- Rule-based AI layer (recommendations, forecasts, insights)
- CSV import (Monobank, PrivatBank, universal)
- Report generation (PDF, CSV, Excel)
- Scheduled tasks (07:30) and notifications (08:00)
- Unit tests for core engines/services
- Dockerfiles (backend + frontend repos)
- GitHub Actions CI (`.github/workflows/backend-ci.yml`)

### Not implemented
- Automated CD (deploy on merge)
- External LLM integration (provider interfaces only)
- Bank API integrations
- Email/Telegram notification delivery
- Audit log
- Backend user settings persistence
- Integration / E2E tests

## Related Projects

- [Flowiq Frontend](https://github.com/YevheniiaDem/flowiq-frontend) — Next.js 16 frontend

## Author

Yevheniia Demchuk

## License

Proprietary — All Rights Reserved
