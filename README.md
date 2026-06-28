# FlowIQ Backend

Spring Boot API for **FlowIQ** — a Ukrainian FOP financial management platform with rule-based insights, forecasts, tasks, notifications, and CSV bank import.

[![Backend CI](https://github.com/YevheniiaDem/flowiq-backend/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/YevheniiaDem/flowiq-backend/actions/workflows/backend-ci.yml)

## Platform repositories

| Repository | Role |
|------------|------|
| **flowiq-backend** (this repo) | REST API, PostgreSQL, Flyway, schedulers |
| [flowiq-frontend](https://github.com/YevheniiaDem/flowiq-frontend) | Next.js 16 web application |
| [flowiq-automation](https://github.com/YevheniiaDem/flowiq-automation) | API/UI/E2E test automation |

## Quick start

```bash
git clone https://github.com/YevheniiaDem/flowiq-backend.git
cd flowiq-backend
docker compose up -d          # PostgreSQL (optional — Spring can auto-start)
./mvnw spring-boot:run
```

| URL | Endpoint |
|-----|----------|
| API | http://localhost:8080/api |
| Swagger | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/api/health |

**Demo login (local dev):** `demo@flowiq.ai` / `demo123`

Clone [flowiq-frontend](https://github.com/YevheniiaDem/flowiq-frontend) and run `npm run dev` for the UI at http://localhost:3000.

## Tech stack

Java 17 · Spring Boot 3.5 · PostgreSQL 15 · Flyway · Spring Security (JWT) · springdoc-openapi · JaCoCo

## Documentation

| Document | Description |
|----------|-------------|
| **[Developer Handbook](docs/DEVELOPER_HANDBOOK.md)** | **Start here** — setup, CI/CD, testing, deployment |
| [Documentation index](docs/index.md) | Full docs map |
| [Architecture](docs/architecture/README.md) | C4 diagrams, flows, module map |
| [SRS](docs/product/SRS.md) | Requirements (as-built) |
| [Security audit](docs/security/SECURITY_AUDIT.md) | Production security review |
| [API overview](docs/api/openapi-overview.md) | REST endpoint inventory |

## Development

```bash
./mvnw test                  # 446+ tests
./mvnw test jacoco:report    # Coverage → target/site/jacoco/
./mvnw clean verify          # CI-equivalent gate
```

Production profile: `SPRING_PROFILES_ACTIVE=prod` with `JWT_SECRET` and database env vars — see [Developer Handbook §14](docs/DEVELOPER_HANDBOOK.md#14-deployment-guide).

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Proprietary — All Rights Reserved. Contact maintainers regarding open-source licensing.

## Author

Yevheniia Demchuk
