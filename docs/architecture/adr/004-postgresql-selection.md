# ADR-004: PostgreSQL Selection

**Status:** Accepted  
**Date:** 2026-06-17  
**Context:** FlowIQ stores relational financial data — users, transactions, tasks, notifications, knowledge articles, import/report jobs, chat history. Data integrity, aggregations, and migrations are first-class requirements.

## Decision

Use **PostgreSQL 15** as the sole production database, accessed via **Spring Data JPA** and **Flyway** migrations.

**Configuration:** `jdbc:postgresql://localhost:5432/flowiq`, Hibernate `ddl-auto=validate`, dialect `PostgreSQLDialect`.

## Why PostgreSQL

| Requirement | PostgreSQL fit |
|-------------|----------------|
| ACID transactions | Full MVCC, reliable commits for financial CRUD + imports |
| Complex aggregations | `SUM`, date ranges, grouping — used heavily in `TransactionRepository` queries |
| Mature Spring support | First-class driver, Flyway PostgreSQL extension, Testcontainers ready |
| JSON future-proofing | JSONB for semi-structured data (import metadata, AI context) — **not used in V1–V5 schema yet** |
| Open source | No licensing cost for MVP/staging |

## Why Not MySQL

| Factor | MySQL gap for FlowIQ |
|--------|---------------------|
| Window functions / analytics | PostgreSQL richer for trend/rolling calculations (forecast engine inputs) |
| Strictness | PostgreSQL stricter type and constraint behavior — preferred for financial schema |
| Ecosystem alignment | Spring Boot + Flyway docs predominantly PostgreSQL-first in greenfield projects |
| Team familiarity | Project already committed: `compose.yaml`, migrations, entities |

MySQL remains viable but offers no compelling advantage for this workload.

## Why Not MongoDB

| Factor | Rejection rationale |
|--------|---------------------|
| Relational model | Core domain is normalized: `users` → `transactions`, FK constraints |
| Aggregations | Revenue/expense sums by month are SQL-native; document aggregation pipelines add complexity |
| JPA investment | Existing `entity/`, `repository/` layer is JPA-centric |
| Transactions | Bank import batch writes benefit from relational ACID, not document eventual consistency |
| Reporting | PDF/Excel reports built from structured `ReportData` — tabular source fits SQL |

MongoDB deferred unless unstructured event streams (audit log, telemetry) justify a polyglot store.

## PostgreSQL Advantages for FlowIQ

### Transactionality

- Import pipeline: parse CSV → categorize → `saveAll(transactions)` in `@Transactional` service
- Flyway migrations applied atomically on startup
- User isolation via `user_id` FK on all tenant-scoped tables

### Analytical Queries

Current repository patterns:

- `sumByUserAndTypeAndDateRange` — monthly revenue/expense for forecasts
- Paginated transaction lists with filters
- Knowledge article full-text candidate queries (JPQL)

PostgreSQL handles these efficiently with B-tree indexes on `(user_id, transaction_date)`.

### JSONB (Future)

Not present in current schema (`V1`–`V5`). Planned uses:

| Use case | JSONB column candidate |
|----------|------------------------|
| Import row errors | `import_jobs.error_details` |
| Report generation metadata | `report_jobs.parameters` |
| Notification payload | `notifications.metadata` |
| AI provider request/response audit | future `ai_invocations` table |

JSONB allows schema flexibility without abandoning relational core.

## Consequences

### Positive

- Single source of truth for all modules
- Flyway + JPA `validate` prevents schema drift
- Docker Compose one-liner for local dev (`postgres:15-alpine`)
- Path to read replicas for reporting if load grows

### Negative

- Vertical scaling first — no built-in sharding
- JSONB not yet leveraged — migration effort when needed
- Connection pool tuning required for production

## Alternatives Considered

1. **H2 for dev, PostgreSQL for prod** — rejected (dialect differences, Flyway parity risk)
2. **SQLite embedded** — rejected (not suitable for multi-user web app)
3. **Supabase / managed Postgres** — compatible; deployment choice, not DB engine change

## Related

- [Database Architecture](../database-architecture.md)
- [ADR-005: Flyway Selection](005-flyway-selection.md)
- [ADR-002: Transaction Seed Strategy](002-transaction-seed-strategy.md)
