# ADR Defense Guide

**Purpose:** Prepare architects, tech leads, and reviewers to defend FlowIQ architectural decisions documented in `docs/architecture/adr/`.  
**As-built verification:** 2026-06-23 against `flowiq-backend` and `flowiq-frontend` source code.  
**ADR index:** [adr/README.md](adr/README.md) · **Coverage gaps:** [adr/ADR_COVERAGE_REPORT.md](adr/ADR_COVERAGE_REPORT.md)

---

## How to Use This Guide

Each ADR section contains:

| Section | Purpose |
|---------|---------|
| **Problem** | What forced the decision |
| **Alternatives considered** | What was evaluated and rejected/deferred |
| **Why chosen** | Rationale for the accepted option |
| **Pros / Cons** | Honest trade-offs |
| **Architectural consequences** | Long-term system impact |
| **Architect questions + recommended answers** | Q&A for review sessions |

**Code anchors** cite real paths so claims are verifiable.

---

## ADR Overview

| ADR | Title | Status | Code anchor |
|-----|-------|--------|-------------|
| [001](adr/001-pluggable-ai-providers.md) | Pluggable AI Providers | Accepted | `AIInsightProvider`, `ForecastProvider`, `*RuleEngine` |
| [002](adr/002-transaction-seed-strategy.md) | Transaction Seed Strategy | Accepted | `TransactionSeedService.seedIfEmpty()` |
| [003](adr/003-ai-quality-factory.md) | Distributed Intelligence Layer | Accepted | `ForecastService`, `AIAccountantService`, engines |
| [004](adr/004-postgresql-selection.md) | PostgreSQL Selection | Accepted | `application.properties`, `compose.yaml` |
| [005](adr/005-flyway-selection.md) | Flyway Selection | Accepted | `db/migration/V1–V5` |
| [006](adr/006-jwt-authentication-strategy.md) | JWT Authentication | Accepted | `JwtService`, `JwtAuthenticationFilter` |
| [007](adr/007-layered-architecture.md) | Layered Backend | Accepted | `controller/` → `service/` → `repository/` |
| [008](adr/008-frontend-architecture.md) | Frontend Architecture | Accepted | `src/features/`, `app/`, `api.ts` |

---

# ADR-001: Pluggable AI Providers

## Problem

FlowIQ must deliver AI-style financial guidance (forecasts, recommendations, categorization, knowledge assist) for Ukrainian FOP users **without**:

- Locking into a single LLM vendor (OpenAI, Anthropic, Gemini)
- Requiring API keys for MVP demos and offline development
- Putting tax/compliance logic solely in non-auditable prompts

The product needs a path to monetize LLM features later while shipping a working rule-based product today.

## Alternatives Considered

| Alternative | Outcome | Reason |
|-------------|---------|--------|
| Direct OpenAI calls inside services | **Rejected** | Tight coupling; every service change touches vendor SDK |
| Single AI gateway microservice | **Deferred** | Operational cost exceeds MVP team size; monolith sufficient |
| Prompt-only, no rules | **Rejected** | Compliance/audit risk for tax and FOP advice |
| One mega `AIService` class | **Rejected** | Becomes god-object; see ADR-003 |

## Why Chosen

**Interface-based providers** with Spring `@Component` auto-discovery:

| Interface | Consumer | Active impl (2026-06-23) |
|-----------|----------|--------------------------|
| `ForecastProvider` | `ForecastService` | `RuleBasedForecastProvider` |
| `KnowledgeProvider` | `KnowledgeService` | `DatabaseKnowledgeProvider` |
| `AIInsightProvider` | `AIAccountantService` | None — rules via `AIRecommendationEngine` |
| `AnalyticsInsightProvider` | `AnalyticsService` | None — field injected, never called |
| `CategorizationProvider` | `CategorizationEngine` | None — `DefaultCategoryRules` in engine |

Rule-based defaults ship **without external APIs**. LLM backends plug in as additional beans without controller changes.

**Code:** `src/main/java/com/flowiq/aiaccountant/AIInsightProvider.java`, `forecasts/provider/ForecastProvider.java`, `AIAccountantService.java` (lines ~103–155).

## Pros

- Demo and CI work **offline** — no LLM spend
- Deterministic outputs — unit-testable (`AIRecommendationEngineTest`, `ForecastEngineTest`)
- Vendor swap or multi-vendor via new `@Component` beans
- FOP/tax thresholds stay in auditable Java, not prompts
- Controllers unchanged when adding `OpenAiForecastProvider`

## Cons

- **Two paths** to maintain: rules + future LLM
- Provider merge rules **differ per service** (must be documented — see [providers.md](../ai/providers.md))
- `AnalyticsInsightProvider` wired but unused — incomplete integration
- Empty provider loops still execute (minor overhead)
- Risk of teams adding LLM without fallback to rules

## Architectural Consequences

- Intelligence remains **domain-distributed** (ADR-003), not centralized
- Production is **100% rule-based** until first provider impl ships
- Future LLM gateway can sit behind provider interfaces, not replace them
- Testing strategy: pure engines without Spring; services with mocked `List<Provider>`
- Compliance narrative: "rules are baseline; LLM augments, does not replace FOP logic"

## Architect Questions & Recommended Answers

**Q1: Is AI real or mocked?**  
**A:** Rule-based and deterministic. No LLM SDK in `pom.xml`. Provider interfaces exist; three of five have zero implementations. See [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md).

**Q2: Why five interfaces instead of one `AIProvider`?**  
**A:** Different input/output contracts: forecast context vs chat vs CSV categorization vs knowledge search. Single interface would force `Object` payloads or god-methods.

**Q3: How do you prevent OpenAI from becoming the only path?**  
**A:** ADR mandates rules-first merge: `AIRecommendationEngine` always runs; providers append. `KnowledgeService` falls back to `DatabaseKnowledgeProvider`. Documented in `ai-architecture.md`.

**Q4: What's the rollout plan for LLM?**  
**A:** Per [future-llm-integration.md](../ai/future-llm-integration.md): implement `KnowledgeProvider` / `AIInsightProvider` first; env-based API keys; rate limits; audit log (not yet built).

**Q5: Why is `AnalyticsInsightProvider` unused?**  
**A:** Extension point prepared; FOP analytics implemented inline in `AnalyticsService`. Technical debt — either invoke providers or remove injection before production.

---

# ADR-002: Transaction Seed Strategy

## Problem

New users register with an empty `transactions` table. Dashboard, forecasts, AI Accountant, analytics, reports, chat, and tasks all aggregate transaction history — **empty state breaks MVP demos and first-login UX**.

## Alternatives Considered

| Alternative | Outcome |
|-------------|---------|
| Frontend-only mocks | Rejected — duplicates backend logic, diverges from API |
| Separate demo tenant | Deferred — per-user seed chosen for simplicity |
| Onboarding wizard empty state | Rejected for MVP — weaker sales demo |
| Seed on registration only | Rejected — lazy seed on first module access |
| In-memory per-request seed | Rejected — breaks schedulers, reports, pagination |

## Why Chosen

`TransactionSeedService.seedIfEmpty(User)`:

- If user has **no transactions** → insert **6 months** synthetic REVENUE/EXPENSE rows
- If user has data → `ensureSixMonthHistory()` backfills zero-revenue months
- Triggered from domain services, not a dedicated onboarding API

**Call sites (verified):** `DashboardService`, `AnalyticsService`, `ForecastService`, `ReportsService`, `ChatService`, `AIAccountantService`, `TaskService`.  
**Not called from:** `TransactionService` (CRUD), `ImportService`.

**Code:** `src/main/java/com/flowiq/service/TransactionSeedService.java`

## Pros

- Instant populated experience on first login
- Same code paths for demo and real data (single `transactions` table)
- Centralized seed logic — one service to gate/disable
- Enables rule engines without manual CSV upload

## Cons

- Users cannot distinguish seeded vs real data (**no `source` column today**)
- Seeded rows persist until deleted
- Hardcoded UAH revenue targets may mislead
- `ensureSixMonthHistory()` may add demo rows to zero-revenue months after partial real usage
- **Runs in all environments** — no `prod` profile disable in code yet

## Architectural Consequences

- All financial metrics may reflect **synthetic data** until user imports/creates real transactions
- Production go-live requires feature flag or profile gate (recommended in ADR, not implemented)
- Audit/compliance reviews must treat seed as **high-risk** for tax advice accuracy
- ADR-002 should be **Superseded** if explicit onboarding replaces auto-seed

## Architect Questions & Recommended Answers

**Q1: Can users trust dashboard numbers on day one?**  
**A:** Not without labeling. First access may show seeded data. Recommend UI banner + `transactions.source = SEED` column (future).

**Q2: Why not seed on registration?**  
**A:** Lazy seed avoids DB write for users who never open financial modules; defers cost until value is shown.

**Q3: How to disable for production?**  
**A:** ADR recommends `flowiq.features.demo-seed-enabled=false` under `prod` profile — **implement before go-live**. Current code always seeds.

---

# ADR-003: AI Quality Factory (Distributed Intelligence)

## Problem

FlowIQ needs multiple intelligence types (forecasts, recommendations, tasks, notifications, categorization, knowledge) without one LLM dependency or one orchestrator class. Teams must extend domains independently.

## Alternatives Considered

| Alternative | Outcome |
|-------------|---------|
| Single `IntelligenceOrchestrator` | Rejected — coupling, hard to test |
| AI microservice | Deferred — monolith sufficient for MVP |
| Event-driven CQRS | Deferred — complexity vs team size |
| Central `AiQualityFactory` class | Rejected — naming exists only in docs; Spring DI is composition layer |

## Why Chosen

Three-level model **as implemented**:

| Level | Role | Examples |
|-------|------|----------|
| L1 Intelligence units | Rule engines, providers | `ForecastEngine`, `AIRecommendationEngine`, `NotificationRuleEngine` |
| L2 Orchestrators | Domain services | `ForecastService`, `AIAccountantService`, `KnowledgeService` |
| L3 Composition | Spring context | `@Autowired(required = false) List<Provider>` |

Governance path (tasks/notifications) runs on **cron**; request-scoped forecast path separate.

**Code:** `docs/architecture/ai-quality-factory.md`, package roots `com.flowiq.forecasts`, `com.flowiq.notifications`, etc.

## Pros

- Domain isolation — forecast math changes don't touch notification rules
- High unit-test coverage on pure engines (`ForecastEngine` ~99%)
- ADR-001 providers attach per domain
- Scheduler isolation from HTTP request path

## Cons

- No single trace entry point for "all intelligence"
- Duplicated FOP/tax constants across services (ADR-009 candidate)
- `AIAccountantService.getForecasts()` duplicates `ForecastEngine` logic (known inconsistency)
- Provider priority rules differ per service

## Architectural Consequences

- Documentation is **mandatory** for operations (see `ai-agents-architecture.md`)
- New intelligence = new engine or provider in domain package, not central registry
- Executive reporting (`ReportsService`) intentionally **outside** AI layer

## Architect Questions & Recommended Answers

**Q1: Where is `AiQualityFactory`?**  
**A:** It doesn't exist. Composition is Spring DI + provider lists. ADR-003 documents the **conceptual** factory as distributed components.

**Q2: How do you debug a wrong recommendation?**  
**A:** Trace domain path: `AIAccountantController` → `AIAccountantService.buildSnapshot()` → `AIRecommendationEngine.generate()`. Deterministic rules — reproduce with same snapshot inputs.

**Q3: Why not one orchestrator for observability?**  
**A:** Accepted trade-off. Mitigation: structured logging per service + future correlation ID in API gateway. Not implemented today.

---

# ADR-004: PostgreSQL Selection

## Problem

FlowIQ stores relational financial data: users, transactions, tasks, notifications, knowledge articles, import/report jobs, chat history. Requirements:

- ACID integrity for imports and CRUD
- Heavy aggregations (monthly revenue/expense, category sums)
- Versioned schema evolution
- Multi-tenant isolation (`user_id` FK)
- Path to production without licensing cost

## Alternatives Considered

| Alternative | Outcome | Key reason |
|-------------|---------|------------|
| **MySQL 8** | Not chosen | No compelling advantage; PostgreSQL already in compose + migrations |
| **MongoDB** | Rejected | Normalized FK model, JPA investment, SQL aggregations core to product |
| **H2 dev + PostgreSQL prod** | Rejected | Dialect drift, Flyway parity risk |
| **SQLite embedded** | Rejected | Multi-user web workload |
| **Supabase / managed Postgres** | Compatible | Deployment choice, not engine change |

### MySQL — why not

- Window/analytics functions weaker for forecast trend inputs
- Stricter constraint behavior preferred for financial schema
- Project already committed: `compose.yaml` (`postgres:15-alpine`), entities, V1–V5 migrations
- Spring + Flyway greenfield docs skew PostgreSQL-first

### MongoDB — why not

- Core model: `users` → `transactions` with FK constraints
- `sumByUserAndTypeAndDateRange` is SQL-native — used in `TransactionRepository`
- Import `saveAll` benefits from relational ACID batches
- JPA layer (`entity/`, `repository/`) not document-oriented
- Mongo deferred unless audit/telemetry event streams justify polyglot store

## Why Chosen

**PostgreSQL 15** as sole RDBMS:

- Spring Data JPA + Hibernate `PostgreSQLDialect`
- `ddl-auto=validate` — Hibernate does not own DDL
- Flyway owns schema (ADR-005)
- Docker: `jdbc:postgresql://localhost:5432/flowiq`

**Code:** `src/main/resources/application.properties` (lines 4–10), `compose.yaml`, `db/migration/V1__initial_schema.sql`.

## Pros

- ACID + MVCC for financial writes
- Rich aggregations and date-range queries
- JSONB available for future semi-structured fields (import errors, AI audit)
- Mature Spring/Flyway/Testcontainers ecosystem
- Open source — no DB license for MVP
- Read replica path for reporting if load grows

## Cons

- Vertical scaling first — no built-in sharding
- JSONB not used in V1–V5 — migration effort when needed
- Connection pool tuning required for production
- Single DB = single point of failure without HA setup

## Architectural Consequences

- All modules share one transactional store — simplifies consistency, complicates blast radius
- Repository query patterns optimized for B-tree indexes on `(user_id, transaction_date)`
- Polyglot persistence (Redis, Elasticsearch) is **additive**, not replacement
- Bank API ingestion (future) writes same `transactions` table

## Architect Questions & Recommended Answers

**Q1: Why PostgreSQL over MySQL for a CRUD app?**  
**A:** Financial aggregations, strict constraints, and existing investment (migrations, compose, team familiarity). MySQL is viable but offers no migration benefit.

**Q2: When would you add MongoDB?**  
**A:** Unstructured high-volume event streams (audit log, AI invocation telemetry) — not for core transactions. ADR-004 allows JSONB in PostgreSQL first.

**Q3: How is tenant isolation enforced?**  
**A:** `user_id` FK on tenant tables; services resolve current user from JWT; repositories filter by user. No `companies` table — single-user tenancy today.

**Q4: Is JSONB on the roadmap?**  
**A:** Yes for `import_jobs.error_details`, `notifications.metadata`, future `ai_invocations` — not in V1–V5. Relational core unchanged.

**Q5: H2 for faster local dev?**  
**A:** Rejected. Dialect differences caused Flyway/Hibernate drift risk. Docker Postgres one-liner is the standard dev path.

**Q6: What about managed PostgreSQL (RDS, Cloud SQL)?**  
**A:** Fully compatible — ADR selects engine, not host. Connection string + secrets in prod profile.

---

# ADR-005: Flyway Selection

## Problem

Schema must evolve safely across local, CI, staging, and production. Hibernate auto-DDL is unsafe for financial data. Team needs reviewable, versioned, reproducible migrations.

## Alternatives Considered

| Alternative | Outcome |
|-------------|---------|
| Hibernate `ddl-auto=update` | **Rejected** — non-auditable, unsafe in prod |
| JPA-only schema generation | **Rejected** — no version history |
| Manual DBA SQL scripts | **Rejected** — environment drift |
| **Liquibase** | **Not chosen** — XML/YAML indirection; team writes SQL directly |
| Flyway Teams undo scripts | **Not configured** — forward-only Community strategy |

### Liquibase — why not

- FlowIQ migrations are plain SQL files (`V5__create_knowledge_articles_table.sql`) — readable in PRs
- Lower learning curve for developers already using SQL
- Spring Boot greenfield culture favors Flyway
- Rollback discipline similar in Community editions — neither auto-rollback

### Manual SQL — why not

- No `flyway_schema_history` audit trail
- Drift between environments
- Forgotten scripts on deploy
- No CI `flyway validate` gate (planned in `.github/workflows/backend-ci.yml`)

## Why Chosen

**Flyway** with SQL migrations:

- Path: `src/main/resources/db/migration/V{n}__{description}.sql`
- Applied on startup: `spring.flyway.enabled=true`
- Current: **V1–V5** (users, transactions, chat, imports, reports, notifications, tasks, knowledge)
- Hibernate: `ddl-auto=validate` only
- Dependencies: `flyway-core`, `flyway-database-postgresql` in `pom.xml`

**Code:** `application.properties` lines 10–18; migration files under `db/migration/`.

## Pros

- Schema is **code** — Git-reviewed, reproducible
- New developer: `./mvnw spring-boot:run` → correct schema
- Aligns with ADR-004 PostgreSQL
- One logical change per migration file
- `flyway_schema_history` shows what ran where

## Cons

- **Forward-only** in Community — no automatic undo
- Editing applied migrations breaks checksums — requires `flyway repair`
- Baseline needed if importing legacy Hibernate-managed DB
- Bad production migration needs compensating `V{n+1}` DDL

## Architectural Consequences

- **Never edit** applied migrations — always add V6+
- PR review must include SQL diff
- CI should run migrations against Testcontainers PostgreSQL (workflow exists)
- Application startup **depends** on migration success — failed migrate blocks deploy
- Data fixes = separate ops runbooks, not schema migrations

### Rollback strategy (defense)

| Scenario | Action |
|----------|--------|
| Bad migration pre-deploy | Fix SQL before merge |
| Bad migration in prod | Deploy V{n+1} reversing DDL + backup restore if needed |
| Local dev reset | `docker compose down -v` |
| Production | Backup before migrate; test on staging copy |

## Architect Questions & Recommended Answers

**Q1: Why Flyway over Liquibase?**  
**A:** SQL-first, one file per version, lower abstraction overhead for current team size. Liquibase adds XML/YAML without benefit here.

**Q2: How do you roll back a bad migration?**  
**A:** Forward-only: new migration reverses DDL. No Flyway Teams undo configured. Restore from backup for data corruption.

**Q3: Who owns DDL — Hibernate or Flyway?**  
**A:** Flyway owns DDL. Hibernate `validate` only — mismatch fails startup (intentional safety).

**Q4: Can developers skip migrations locally?**  
**A:** No — startup runs Flyway. Drift detected via validate errors.

**Q5: How many migrations today?**  
**A:** V1–V5 in `src/main/resources/db/migration/`. V1 initial schema; V2 auto_categorized; V3–V5 notifications, tasks, knowledge.

**Q6: Migration in CI?**  
**A:** `.github/workflows/backend-ci.yml` builds and tests backend; extend with Testcontainers migrate gate for full parity.

---

# ADR-006: JWT Authentication Strategy

## Problem

FlowIQ is a stateless REST API consumed by a Next.js SPA (and potentially mobile later). Need authentication that:

- Scales horizontally without sticky sessions
- Works cross-origin (Vercel frontend → API backend)
- Integrates with OpenAPI/Swagger
- Avoids Redis session cluster for MVP

## Alternatives Considered

| Alternative | Outcome |
|-------------|---------|
| Server-side sessions + Redis | Deferred — ops overhead |
| OAuth2 / Keycloak | Deferred — enterprise SSO not required |
| API keys only | Rejected — no user identity |
| Opaque tokens + introspection | Deferred — extra auth hop |
| httpOnly cookie sessions | Partially considered — Bearer chosen for CORS simplicity |

## Why Chosen

**Stateless JWT** with Spring Security:

| Component | Role |
|-----------|------|
| `JwtService` | Generate/validate HS256 tokens |
| `JwtAuthenticationFilter` | Bearer header on protected routes |
| BCrypt | Password hashing via `UserDetailsService` |
| Access token | 24h (`jwt.access-token-expiration=86400000`) |
| Refresh token | 7d — generated on login, **refresh endpoint not implemented** |

**Frontend:** `flowiq-frontend/src/services/api.ts` — `localStorage` token, axios interceptor, 401 → logout.

**Code:** `JwtService.java`, `JwtAuthenticationFilter.java`, `application.properties` lines 39–44.

## Pros

- Horizontal scale — any instance validates JWT
- SPA-friendly Bearer header
- No session store for MVP
- Swagger "Authorize" standard pattern
- Cloud/container friendly

## Cons

- **No refresh endpoint** — user re-logs after 24h access expiry
- Refresh token stored client-side but unused
- JWT in `localStorage` — XSS theft risk
- Symmetric HS256 secret in properties — must externalize for prod
- Logout is client-only — token valid until expiry
- Roles (`ADMIN`/`USER`/`VIEWER`) partially enforced

## Architectural Consequences

- Phase 2 required before production: `POST /api/auth/refresh`
- Phase 3: `jti` denylist, refresh rotation, secret manager
- Authorization gaps need ADR-017 (RBAC enforcement)
- Alternative cookie-based auth would require ADR amendment + CORS/SameSite design

## Architect Questions & Recommended Answers

**Q1: Why JWT instead of session cookies?**  
**A:** Cross-origin SPA (localhost:3000 / Vercel → :8080 API). Bearer avoids SameSite cookie complexity. Trade-off: harder revocation.

**Q2: Is refresh token implemented?**  
**A:** Generated and stored client-side; **`POST /api/auth/refresh` does not exist**. Phase 2 on roadmap in ADR-006.

**Q3: How do you revoke on logout?**  
**A:** Client clears `localStorage`. Server cannot invalidate until denylist (Phase 3) or short access TTL.

**Q4: Why 24-hour access token?**  
**A:** MVP convenience. High-security deployments should shorten to 15–60 minutes after refresh flow ships.

**Q5: XSS risk with localStorage?**  
**A:** Acknowledged. Mitigations: CSP, input sanitization, consider httpOnly cookie migration for prod.

**Q6: Why HS256 not RS256?**  
**A:** Single monolith validates tokens — symmetric key sufficient for MVP. RS256 when multiple services verify tokens.

---

# ADR-007: Layered Architecture (Backend)

## Problem

Spring Boot monolith with 15+ REST endpoints, multiple domain packages (forecasts, tasks, notifications, knowledge), growing business logic. Structure must support:

- Parallel team work
- Unit testing without full Spring context
- Clear placement for new endpoints
- Integration with ADR-001 providers and ADR-003 intelligence units

## Alternatives Considered

| Alternative | Outcome |
|-------------|---------|
| Hexagonal (ports/adapters) | Deferred — ceremony exceeds MVP team |
| CQRS + event sourcing | Rejected — CRUD-heavy app |
| Controller → Repository direct | Rejected — fat controllers, untestable |
| Microservices per module | Rejected — operational cost |
| Anemic entities + rich services | **Accepted implicitly** — see cons |

## Why Chosen

Classic **Controller → Service → Repository → Entity**:

```
com.flowiq/
├── controller/           # HTTP, @Valid, OpenAPI
├── service/              # Orchestration, @Transactional
├── repository/           # Spring Data JPA
├── entity/               # JPA mappings
├── dto/                  # API contracts
├── config/, security/, exception/
└── {forecasts,tasks,knowledge,...}/   # Vertical slices
```

**Rule engines** (`@Component`) injected into services — never called from controllers directly.

**Example vertical slice:** `com.flowiq.forecasts/` — `controller`, `service`, `engine`, `provider`, `dto`.

**Code:** 13 controllers under `src/main/java/com/flowiq/`; services call repositories; `DashboardController` → `DashboardService` → `TransactionRepository`.

## Pros

- Predictable location for new code
- Spring Boot conventions — low onboarding friction
- Services unit-tested with mocked repos (`ForecastServiceTest`, etc.)
- DTOs decouple API from JPA entities
- Enables ADR-003 domain orchestrators without new paradigm
- Thin controllers — business logic in services/engines

## Cons

- **Anemic domain model** — logic in services, not entities
- Cross-service calls (`ReportsService` → `AnalyticsService`) — cycle risk
- FOP constants duplicated across services
- Some services large (`AIAccountantService`, `AnalyticsService`)
- Not full hexagonal — external systems (future LLM) couple via provider interfaces in service layer

## Architectural Consequences

- New feature checklist: Controller method → Service method → Repository query → DTO
- Domain packages grow as **vertical slices within layers**, not pure DDD bounded contexts
- Integration tests deferred — unit tests on services/engines today
- Microservice extraction path: lift domain package + its DB tables — not planned for MVP

## Architect Questions & Recommended Answers

**Q1: Why not hexagonal architecture?**  
**A:** Team size and MVP speed. Provider interfaces (ADR-001) give partial ports without full adapter ceremony. Revisit if external integrations multiply.

**Q2: Where does business logic live?**  
**A:** Services orchestrate; pure rules in `@Component` engines (`ForecastEngine`, `AIRecommendationEngine`). Entities are mostly data holders.

**Q3: How do you prevent circular service dependencies?**  
**A:** Discipline + code review. Known pattern: `AIAccountantService` → `AnalyticsService` for FOP data. Extract shared `FopCalculationService` if cycles appear (ADR-009).

**Q4: Why are rule engines not in the service class?**  
**A:** Testability and single responsibility. `ForecastEngine` has 99%+ coverage without Spring mocks.

**Q5: How does this map to REST?**  
**A:** One controller per module (`ForecastController`, `AIAccountantController`). Mirrors frontend feature folders (ADR-008).

**Q6: Controller calling repository directly?**  
**A:** Anti-pattern per ADR. Grep reviews should flag direct repository injection in controllers.

---

# ADR-008: Frontend Architecture

## Problem

Bilingual (UK/EN) financial dashboard SPA needs:

- Fast iteration across 12+ modules
- Structure mirroring backend domains
- JWT attachment and preference headers
- Deploy to Vercel and Docker
- Clear boundaries without over-engineering state

## Alternatives Considered

| Alternative | Outcome |
|-------------|---------|
| CRA / Vite SPA | Rejected — Next.js routing, Vercel, standalone Docker |
| Redux Toolkit global store | Rejected — unnecessary complexity |
| TanStack Query | Not adopted — hooks + useEffect sufficient for MVP |
| Monolithic `components/` folder | Rejected — doesn't scale |
| tRPC / GraphQL | Rejected — backend is OpenAPI REST |
| Server Components for data | Not used — client components call REST |

## Why Chosen

**Next.js 16 App Router** + feature folders:

```
src/features/{module}/   components/, hooks/, services/
src/services/              api.ts, auth, dashboard, chat
src/shared/                context, i18n, theme, ui
app/                       routes → feature views
```

**Stack:** React 19, TypeScript, Tailwind 4, Radix UI, axios, Recharts.

**State:**

| State | Mechanism |
|-------|-----------|
| Auth | `localStorage` + `api.ts` interceptor |
| Language/currency/theme | `PreferencesContext` |
| Module data | Feature hooks (`useForecasts`, `useAnalytics`) — fetch on mount |
| No Redux / React Query | MVP scope |

**Code:** `flowiq-frontend/src/features/`, `src/services/api.ts`, `app/` routes.

## Pros

- Feature colocation — `forecast.service.ts` beside `useForecasts.ts`
- 1:1 backend parity (`/api/forecasts` → `features/forecasts`)
- Shared `apiClient` — JWT + `X-App-Language` + `X-App-Currency`
- `output: "standalone"` for Docker
- TypeScript DTO alignment with backend

## Cons

- **No global server cache** — repeat navigation refetches
- **No Next.js middleware auth** — client-side redirect only
- Hybrid mocks: `tax-profile.service.ts`, Business Guide static data
- Hook patterns inconsistent across modules
- **Zero frontend tests** today
- JWT in localStorage (ADR-006 XSS concern)

## Architectural Consequences

- Adding TanStack Query is natural upgrade without folder restructure
- Middleware auth guard recommended before production
- Mock hybrid must be documented per module ([data-sources.md](data-sources.md))
- i18n via headers (`X-App-Language`), not DB — matches `AppPreferencesFilter`

## Architect Questions & Recommended Answers

**Q1: Why Next.js if everything is client-side REST?**  
**A:** App Router file-based routing, Vercel deployment, standalone Docker output, ecosystem. SSR not required for authenticated dashboard MVP.

**Q2: Why no React Query?**  
**A:** Modules load independently; no complex cache invalidation graph. Add when stale-while-revalidate and deduplication matter.

**Q3: How does auth work on the frontend?**  
**A:** `api.ts` attaches Bearer token; 401 clears storage and redirects. No server middleware — gap for hardening.

**Q4: Why feature folders vs layers (components/services)?**  
**A:** Scales with 12+ modules; team ownership per feature; matches backend module boundaries.

**Q5: Where is data mocked?**  
**A:** Tax profile card, Business Guide groups/taxes/KVED, integrations list — static `mock-data/`. Dashboard, forecasts, AI Accountant call **real API**.

**Q6: How does frontend map to ADR-007?**  
**A:** `*.service.ts` = API client layer; hooks = presentation orchestration; no business rules — all intelligence on backend.

---

# Top 30 Architecture Questions About ADR Decisions

| # | Question | ADR | Recommended answer (short) |
|---|----------|-----|---------------------------|
| 1 | Is the AI real or rule-based? | 001, 003 | 100% rule-based in production; five provider interfaces; no LLM SDK. |
| 2 | Why not call OpenAI directly? | 001 | Vendor lock-in, offline demos, compliance — providers abstract vendor. |
| 3 | When will LLM ship? | 001 | After `AIInsightProvider` / `KnowledgeProvider` impl + audit log; see future-llm-integration.md. |
| 4 | Why are dashboard numbers non-zero for new users? | 002 | `TransactionSeedService` auto-seeds 6 months demo transactions. |
| 5 | Is demo data labeled? | 002 | **No** — `source` column not implemented; UI banner recommended. |
| 6 | How to disable seed in production? | 002 | Feature flag `demo-seed-enabled=false` — **not in code yet**; must add before go-live. |
| 7 | Where is `AiQualityFactory`? | 003 | Doesn't exist — Spring DI + distributed services/engines. |
| 8 | Why no central intelligence orchestrator? | 003 | Coupling, testing, team parallelism — domain packages isolated. |
| 9 | Why PostgreSQL not MySQL? | 004 | Aggregations, constraints, existing migrations — no migration benefit to switch. |
| 10 | Why not MongoDB for flexibility? | 004 | Relational FK model, JPA, SQL sums — JSONB in Postgres first for semi-structured. |
| 11 | How is multi-tenancy enforced? | 004, 007 | `user_id` on rows; JWT identifies user; no company table yet. |
| 12 | Why Flyway not Liquibase? | 005 | SQL-first, simpler for team, one file per version. |
| 13 | How do you rollback migrations? | 005 | Forward-only Community — compensating V{n+1} + backup restore. |
| 14 | Hibernate vs Flyway ownership? | 005 | Flyway DDL; Hibernate `validate` only. |
| 15 | Why JWT not sessions? | 006 | Stateless scale, SPA cross-origin, no Redis for MVP. |
| 16 | Is refresh token working? | 006 | Generated but **`/api/auth/refresh` missing** — Phase 2. |
| 17 | JWT in localStorage — XSS? | 006, 008 | Acknowledged risk; CSP + consider httpOnly cookies for prod. |
| 18 | How is logout handled? | 006 | Client-side clear; token valid until expiry — denylist Phase 3. |
| 19 | Are roles enforced? | 006, 007 | Partially — ADR-017 needed for full RBAC. |
| 20 | Why Controller-Service-Repository? | 007 | Spring convention, testability, thin controllers. |
| 21 | Why not hexagonal? | 007 | MVP team size; provider interfaces give partial decoupling. |
| 22 | Where do FOP tax rules live? | 007, 003 | Duplicated in `AnalyticsService`, `ForecastService`, rule engines — ADR-009 candidate. |
| 23 | Why Next.js for a dashboard SPA? | 008 | Routing, Vercel, Docker standalone — not for SSR requirements. |
| 24 | Why no Redux/React Query? | 008 | MVP complexity; hooks sufficient; Query is future option. |
| 25 | Is frontend data all from API? | 008, 002 | Mostly yes; tax profile + parts of Business Guide are static mocks. |
| 26 | How does i18n work? | 008 | `PreferencesContext` + `X-App-Language` header → `AppPreferencesFilter`. |
| 27 | CI/CD for migrations and tests? | 005, 007 | `backend-ci.yml` added; Testcontainers migrate gate recommended. |
| 28 | Can we swap database engine? | 004, 005 | Theoretically yes — practically high cost; JPA + Flyway SQL are PostgreSQL-specific. |
| 29 | Path to microservices? | 003, 007 | Lift domain package (e.g. `forecasts/`) — not planned; monolith correct for MVP. |
| 30 | What ADRs are missing? | Coverage report | FOP constants (009), RBAC (017), audit log (013), CI/CD formal ADR when pipeline matures. |

---

## Pre-Review Checklist

Before an architect review session:

- [ ] Read [ADR_COVERAGE_REPORT.md](adr/ADR_COVERAGE_REPORT.md) for undocumented decisions
- [ ] Read [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md) for intelligence layer truth
- [ ] Confirm environment: seed disabled strategy for prod discussion (ADR-002)
- [ ] Confirm JWT Phase 2 timeline (ADR-006)
- [ ] Prepare demo: show `flyway_schema_history`, provider interface grep, `seedIfEmpty` call sites

---

## Related Documents

| Document | Link |
|----------|------|
| ADR Index | [adr/README.md](adr/README.md) |
| ADR Coverage | [adr/ADR_COVERAGE_REPORT.md](adr/ADR_COVERAGE_REPORT.md) |
| Architecture Review Readiness | [ARCHITECTURE_REVIEW_READINESS.md](ARCHITECTURE_REVIEW_READINESS.md) |
| AI Audit | [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md) |
| Data Sources | [data-sources.md](data-sources.md) |

**Prepared:** 2026-06-23  
**Next update:** When ADR-009+ accepted or ADR-006 Phase 2 ships
