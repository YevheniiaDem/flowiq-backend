# Technical Debt Register

**Audit date:** 2026-06-23  
**Source of truth:** `flowiq-backend` and `flowiq-frontend` code only  
**Method:** Static analysis — grep, schema review, workflow inspection, cross-check with [ADR Coverage Report](adr/ADR_COVERAGE_REPORT.md)

---

## Summary

| Severity | Count | Production impact |
|----------|-------|-------------------|
| **Critical** | 6 | Blocks trustworthy production launch |
| **High** | 14 | Security, compliance, or release pipeline gaps |
| **Medium** | 16 | Maintainability, ops maturity, partial features |
| **Low** | 12 | Quality-of-life, documentation, future scale |

---

## Critical

### TD-C01 — Auto-seed demo transactions in all environments

| Field | Detail |
|-------|--------|
| **Description** | `TransactionSeedService.seedIfEmpty()` inserts 6 months of synthetic transactions when a user has no data. No profile flag disables this in production. |
| **Location** | `src/main/java/com/flowiq/service/TransactionSeedService.java`; callers: `DashboardService`, `AnalyticsService`, `ForecastService`, `ReportsService`, `ChatService`, `AIAccountantService`, `TaskService` |
| **System impact** | Dashboard, forecasts, tax/FOP metrics, AI recommendations, and reports may reflect **fake revenue** on first login. Users cannot tell demo from real data. |
| **Risk** | **Critical** — incorrect tax/FOP guidance; regulatory and trust failure |
| **Fix complexity** | **Medium** — feature flag + UI labeling + optional `transactions.source` column |
| **Recommended solution** | Add `flowiq.features.demo-seed-enabled` (default `false` for `prod` profile); UI banner until first real import/transaction; ADR-002 production disable criteria |

---

### TD-C02 — No audit log

| Field | Detail |
|-------|--------|
| **Description** | No `audit_log` table, entity, repository, service, or API. No record of who changed transactions, generated reports, or received AI/tax advice. |
| **Location** | Absent from `src/main/resources/db/migration/V1–V5`; no `*Audit*` classes under `src/main/java` |
| **System impact** | Cannot prove compliance, investigate incidents, or trace AI/financial advice for a user |
| **Risk** | **Critical** for financial SaaS and future LLM features |
| **Fix complexity** | **High** — schema, async writer, API, retention policy |
| **Recommended solution** | ADR-013; `V6__create_audit_log.sql`; `AuditLogService` on sensitive operations (auth, transactions CRUD, imports, reports, AI Accountant chat) |

---

### TD-C03 — JWT secret and credentials in default properties

| Field | Detail |
|-------|--------|
| **Description** | `jwt.secret`, DB password, and `spring.jpa.show-sql=true` committed in `application.properties`. No `application-prod.properties`. |
| **Location** | `src/main/resources/application.properties` (lines 4–7, 11, 39–40) |
| **System impact** | Production deploy with default config leaks schema in logs and uses forgeable dev JWT secret |
| **Risk** | **Critical** — token forgery, credential exposure |
| **Fix complexity** | **Low–Medium** — env vars, Spring profiles, secrets manager |
| **Recommended solution** | `application-prod.properties` with env placeholders; fail startup if `jwt.secret` is default in prod; disable `show-sql` in prod |

---

### TD-C04 — Demo transaction data not distinguishable in schema

| Field | Detail |
|-------|--------|
| **Description** | `transactions` has `auto_categorized` (V2) but **no `source`** (`SEED` / `IMPORT` / `MANUAL`). Seeded rows identical to user-created rows. |
| **Location** | `V1__initial_schema.sql`, `V2__add_auto_categorized_column.sql`; `TransactionSeedService` |
| **System impact** | Cannot filter, delete, or label demo data; analytics aggregate seeded + real indiscriminately |
| **Risk** | **Critical** combined with TD-C01 |
| **Fix complexity** | **Medium** — migration + backfill + repository filters |
| **Recommended solution** | `V6` add `source VARCHAR(20)`; set on seed/import/manual CRUD; expose in API for UI badge |

---

### TD-C05 — Financial/tax constants hardcoded in multiple services

| Field | Detail |
|-------|--------|
| **Description** | FOP income limits, single tax rates, ESV amounts duplicated in `AnalyticsService`, `ForecastService`, `NotificationRuleEngine`, `TaskRuleEngine` (and related logic). |
| **Location** | e.g. `AnalyticsService.java` lines 34–44 (`INCOME_LIMITS`, `SINGLE_TAX_RATES`, `ESV_MONTHLY`); same pattern in `ForecastService`, `NotificationRuleEngine`, `TaskRuleEngine` |
| **System impact** | Law changes require multi-file edits; risk of **inconsistent tax numbers** across modules |
| **Risk** | **Critical** for tax advice accuracy |
| **Fix complexity** | **Medium** — `TaxConfigurationService` or DB/config table |
| **Recommended solution** | ADR-009; single source of truth; versioned config with effective dates |

---

### TD-C06 — No server-side token revocation / refresh flow incomplete

| Field | Detail |
|-------|--------|
| **Description** | Refresh tokens issued on login but **`POST /api/auth/refresh` does not exist**. `AuthController.logout()` returns 204 with **no server action**. Access token valid until expiry (24h). |
| **Location** | `AuthController.java` (no `/refresh`); `AuthService.java`; `JwtAuthenticationFilter.java`; `flowiq-frontend/src/services/api.ts` (401 clears tokens, no refresh attempt) |
| **System impact** | Stolen token usable for 24h; refresh token stored in `localStorage` with no use path |
| **Risk** | **Critical** for production security posture |
| **Fix complexity** | **Medium** — refresh endpoint, axios interceptor, optional denylist (Phase 3) |
| **Recommended solution** | ADR-006 Phase 2: `POST /api/auth/refresh`, validate `type=refresh`, shorten access TTL after refresh works |

---

## High

### TD-H01 — User Settings backend missing

| Field | Detail |
|-------|--------|
| **Description** | Settings UI persists language/currency/theme in **browser only** (`PreferencesContext` + `localStorage`). Profile, notifications, and security sections are **UI placeholders** with no API. |
| **Location** | `flowiq-frontend/src/features/settings/components/SettingsView.tsx`; no `SettingsController` in backend |
| **System impact** | Preferences lost on new device; no server-side user profile or notification preferences |
| **Risk** | **High** — poor UX; cannot enforce locale for server-generated messages per user |
| **Fix complexity** | **Medium** — `user_settings` table or JSONB on `users`, REST CRUD |
| **Recommended solution** | `GET/PATCH /api/settings`; sync `PreferencesContext` on login; persist profile fields on `users` |

---

### TD-H02 — Refresh Token endpoint (explicit gap)

| Field | Detail |
|-------|--------|
| **Description** | `JwtService.generateRefreshToken()` and `AuthResponse.refreshToken` exist; no consumer endpoint. |
| **Location** | `JwtService.java`, `AuthService.java`, `AuthController.java` |
| **System impact** | Users forced to re-login daily; refresh token is dead weight in client storage |
| **Risk** | **High** — session UX and security design incomplete |
| **Fix complexity** | **Medium** |
| **Recommended solution** | Same as TD-C06 Phase 2 |

---

### TD-H03 — RBAC not enforced

| Field | Detail |
|-------|--------|
| **Description** | `User.Role` enum (`ADMIN`, `USER`, `VIEWER`) and JWT `role` claim exist; **no `@PreAuthorize`** on controllers. `SecurityConfig` only checks authenticated vs public. |
| **Location** | `User.java`, `UserPrincipal.getAuthorities()`, `SecurityConfig.java`; `docs/security/authorization.md` lists TODOs |
| **System impact** | Any authenticated user can access all endpoints; role field is cosmetic |
| **Risk** | **High** when admin or multi-user features ship |
| **Fix complexity** | **Medium** — method security + endpoint matrix |
| **Recommended solution** | ADR-017; `@EnableMethodSecurity`; enforce `user_id` ownership on all tenant data (verify per controller) |

---

### TD-H04 — No staging environment

| Field | Detail |
|-------|--------|
| **Description** | `docs/deployment/environments.md` marks staging **TBD** for frontend, backend, and database. No `application-staging.properties`, no staging workflow. |
| **Location** | `docs/deployment/environments.md`; no staging infra in repo |
| **System impact** | Migrations and releases tested only locally or directly toward prod |
| **Risk** | **High** — Flyway mistakes or config errors hit production first |
| **Fix complexity** | **High** — infra + env config + data anonymization policy |
| **Recommended solution** | Managed PostgreSQL staging; deploy backend container; Vercel preview or staging URL; run Flyway on staging before prod |

---

### TD-H05 — No CD (continuous deployment) pipeline

| Field | Detail |
|-------|--------|
| **Description** | CI exists (`backend-ci.yml`, `frontend-ci.yml`) — **verify/build only**. No deploy workflow, no container registry push, no Vercel/GitHub Environments integration. |
| **Location** | `flowiq-backend/.github/workflows/backend-ci.yml`; `flowiq-frontend/.github/workflows/frontend-ci.yml` |
| **System impact** | Manual, error-prone releases; no automated rollback |
| **Risk** | **High** for repeatable production releases |
| **Fix complexity** | **High** — secrets, environments, deployment targets |
| **Recommended solution** | ADR-019; CD to staging on merge; manual approval to prod; document in `ci-cd-as-built.md` |

---

### TD-H06 — No frontend tests

| Field | Detail |
|-------|--------|
| **Description** | Zero `*.test.ts`, `*.spec.ts`, Vitest, or Jest config in `flowiq-frontend`. `package.json` has no test script. |
| **Location** | `flowiq-frontend/package.json`; glob search returns 0 test files |
| **System impact** | Regressions in hooks, services, and forms caught only manually |
| **Risk** | **High** as UI surface grows (12+ modules) |
| **Fix complexity** | **Medium** — Vitest + RTL; start with `api.ts`, `auth.service.ts`, hooks |
| **Recommended solution** | Add `vitest`; test `forecast.service.ts`, `useForecasts`, `PreferencesContext`; CI `npm test` |

---

### TD-H07 — No E2E tests

| Field | Detail |
|-------|--------|
| **Description** | No Playwright, Cypress, or E2E folder in either repo. |
| **Location** | Absent from both repositories |
| **System impact** | Critical flows (login → dashboard → import) not automated |
| **Risk** | **High** before production — auth and seed behavior untested end-to-end |
| **Fix complexity** | **High** — test DB, CI job, stable selectors |
| **Recommended solution** | Playwright against docker-compose (Postgres + backend + frontend); smoke: register, login, dashboard stats |

---

### TD-H08 — No observability stack

| Field | Detail |
|-------|--------|
| **Description** | No `spring-boot-starter-actuator`, Micrometer, Prometheus, OpenTelemetry, or structured correlation IDs in backend. No frontend error tracking (Sentry, etc.). |
| **Location** | `pom.xml` — no actuator dependency; `docs/operations/monitoring.md` is checklist only |
| **System impact** | Cannot measure latency, errors, or JVM health in production |
| **Risk** | **High** — blind operations |
| **Fix complexity** | **Medium** — actuator + secure endpoints + log JSON |
| **Recommended solution** | Add actuator (`/actuator/health`, `/actuator/prometheus`); request ID filter; optional Sentry on frontend |

---

### TD-H09 — No production monitoring / alerting

| Field | Detail |
|-------|--------|
| **Description** | No uptime checks, alert rules, or APM wired in code or repo. Docker HEALTHCHECK hits `/api/health` only at container level. |
| **Location** | `Dockerfile` HEALTHCHECK; `HealthController.java`; no external monitoring config |
| **System impact** | Outages discovered by users, not operators |
| **Risk** | **High** post-launch |
| **Fix complexity** | **Medium** — depends on hosting (Uptime Robot, Grafana Cloud, etc.) |
| **Recommended solution** | External probe on `/api/health`; alert on 5xx rate; log aggregation (CloudWatch, Loki) |

---

### TD-H10 — JWT in localStorage (XSS surface)

| Field | Detail |
|-------|--------|
| **Description** | Frontend stores `token`, `refreshToken`, `user` in `localStorage`. No httpOnly cookies. |
| **Location** | `flowiq-frontend/src/services/api.ts`, `auth.service.ts` |
| **System impact** | XSS can exfiltrate bearer tokens |
| **Risk** | **High** — standard SPA trade-off, must mitigate |
| **Fix complexity** | **High** if moving to cookies (CORS/SameSite redesign) |
| **Recommended solution** | Strict CSP; sanitize outputs; long-term evaluate BFF + httpOnly cookie pattern |

---

### TD-H11 — No integration tests with PostgreSQL

| Field | Detail |
|-------|--------|
| **Description** | Backend tests are unit tests with mocks. Only `FlowiqBackendApplicationTests` uses `@SpringBootTest` (requires live DB). No Testcontainers. CI runs `mvn verify` with `SPRING_DOCKER_COMPOSE_ENABLED=false` — Flyway/JPA not validated in CI against real Postgres. |
| **Location** | `src/test/java/**` (10 test classes); `.github/workflows/backend-ci.yml` |
| **System impact** | Migration SQL errors or repository query regressions may reach runtime undetected |
| **Risk** | **High** for schema evolution |
| **Fix complexity** | **Medium** — Testcontainers PostgreSQL in CI |
| **Recommended solution** | `@Testcontainers` + `PostgreSQLContainer`; run Flyway + 2–3 repository integration tests in CI |

---

### TD-H12 — Demo user seeded on every application start

| Field | Detail |
|-------|--------|
| **Description** | `DemoUserSeedService` creates `demo@flowiq.ai` / `demo123` on startup if missing — **all environments**. |
| **Location** | `src/main/java/com/flowiq/service/DemoUserSeedService.java` |
| **System impact** | Known credentials in production if not disabled; security scan finding |
| **Risk** | **High** in production |
| **Fix complexity** | **Low** — profile guard or remove in prod |
| **Recommended solution** | `@Profile("!prod")` or explicit `flowiq.demo-user.enabled=false` in prod |

---

### TD-H13 — Swagger/OpenAPI publicly accessible

| Field | Detail |
|-------|--------|
| **Description** | `/swagger-ui/**` and `/v3/api-docs/**` are `permitAll()` in `SecurityConfig`. |
| **Location** | `SecurityConfig.java` lines 44–47 |
| **System impact** | Full API surface exposed without auth in any deployed environment |
| **Risk** | **High** for production reconnaissance |
| **Fix complexity** | **Low** — restrict to dev/staging profile |
| **Recommended solution** | Move Swagger to `@Profile("dev")` or require ADMIN role in prod |

---

### TD-H14 — ADR gaps for production-critical decisions

| Field | Detail |
|-------|--------|
| **Description** | Undocumented ADRs: tax config (009), audit log (013), RBAC (017), CI/CD (019), authorization, staging strategy. |
| **Location** | `docs/architecture/adr/ADR_COVERAGE_REPORT.md` |
| **System impact** | Architectural decisions implicit in code only — review and onboarding friction |
| **Risk** | **High** for architect sign-off |
| **Fix complexity** | **Low–Medium** — write ADRs as decisions are implemented |
| **Recommended solution** | Prioritize ADR-009, 013, 017, 019 before go-live |

---

## Medium

### TD-M01 — `AnalyticsInsightProvider` injected but never used

| Field | Detail |
|-------|--------|
| **Description** | `AnalyticsService` stores `insightProviders` list; no method reads it. |
| **Location** | `AnalyticsService.java` lines 49–60 |
| **System impact** | Dead extension point; misleading architecture docs |
| **Risk** | **Medium** — confusion, bit rot |
| **Fix complexity** | **Low** — invoke or remove field |
| **Recommended solution** | Remove until LLM analytics ships, or call in `getFopInsights()` merge |

---

### TD-M02 — `TransactionInsightService` has zero callers

| Field | Detail |
|-------|--------|
| **Description** | `@Service` with `buildAnalysisContext()` — no references outside its class. |
| **Location** | `TransactionInsightService.java` |
| **System impact** | Dead code; dashboard uses inline rules instead |
| **Risk** | **Low–Medium** |
| **Fix complexity** | **Low** |
| **Recommended solution** | Wire into future LLM path or delete until needed |

---

### TD-M03 — Duplicate forecast logic (AI Accountant vs Forecast Center)

| Field | Detail |
|-------|--------|
| **Description** | `AIAccountantService.buildForecast()` is separate from `ForecastEngine` / `ForecastService`. |
| **Location** | `AIAccountantService.java` ~383–403 vs `ForecastService.java` + `ForecastEngine.java` |
| **System impact** | Inconsistent forecast numbers between `/api/ai-accountant/forecasts` and `/api/forecasts/*` |
| **Risk** | **Medium** — user trust |
| **Fix complexity** | **Medium** — delegate to `ForecastService` |
| **Recommended solution** | Reuse `ForecastService` horizons or shared calculator |

---

### TD-M04 — Duplicate business health scoring

| Field | Detail |
|-------|--------|
| **Description** | `DashboardService.calculateHealthScore()` (margin-based) ≠ `AIAccountantService.calculateHealthScore()` (rule adjustments). |
| **Location** | `DashboardService.java` ~287; `AIAccountantService.java` ~418 |
| **System impact** | Different health scores on dashboard vs AI Accountant for same user |
| **Risk** | **Medium** |
| **Fix complexity** | **Medium** — extract shared `HealthScoreService` |
| **Recommended solution** | Single scoring module or document intentional difference in UI |

---

### TD-M05 — Frontend mock hybrid (Business Guide + tax profile)

| Field | Detail |
|-------|--------|
| **Description** | Articles use API; FOP groups, taxes, KVED, profile use static `mock*` in `business-guide.service.ts`. Tax profile card uses `mock-data/tax-profile.localized`. |
| **Location** | `flowiq-frontend/src/features/business-guide/services/business-guide.service.ts`; `src/services/tax-profile.service.ts` |
| **System impact** | UI shows static tax/FOP data alongside live API metrics — inconsistent trust |
| **Risk** | **Medium** |
| **Fix complexity** | **Medium–High** — backend endpoints or CMS for static content |
| **Recommended solution** | ADR-014; migrate to API or label sections "reference data" |

---

### TD-M06 — Notification EMAIL/TELEGRAM channels not implemented

| Field | Detail |
|-------|--------|
| **Description** | `NotificationChannel` enum includes EMAIL, TELEGRAM; generator always sets `IN_APP`. |
| **Location** | `NotificationChannel.java`, `NotificationGeneratorService.java` |
| **System impact** | Users only see in-app notifications |
| **Risk** | **Medium** — product expectation gap |
| **Fix complexity** | **High** — SMTP/Telegram bot integration |
| **Recommended solution** | Defer or implement email provider behind interface |

---

### TD-M07 — No API rate limiting

| Field | Detail |
|-------|--------|
| **Description** | No Bucket4j, Resilience4j rate limiter, or gateway throttling in codebase. |
| **Location** | Absent from `SecurityConfig`, controllers |
| **System impact** | Auth and expensive endpoints vulnerable to abuse |
| **Risk** | **Medium** |
| **Fix complexity** | **Medium** |
| **Recommended solution** | Rate limit `/api/auth/login`, file upload, report generation |

---

### TD-M08 — Client-side auth guard only

| Field | Detail |
|-------|--------|
| **Description** | No Next.js `middleware.ts`. Protected routes rely on `MainLayout` `useEffect` + `authService.isAuthenticated()` (token presence in localStorage). |
| **Location** | `MainLayout.tsx`; no `middleware.ts` in frontend |
| **System impact** | Flash of protected content possible; no server-side route protection |
| **Risk** | **Medium** |
| **Fix complexity** | **Low–Medium** |
| **Recommended solution** | Next.js middleware checking cookie/token for `/dashboard` routes |

---

### TD-M09 — Docker image build skips tests

| Field | Detail |
|-------|--------|
| **Description** | Backend `Dockerfile` runs `mvn package -DskipTests`. |
| **Location** | `Dockerfile` line 10 |
| **System impact** | Image may be built from code that fails unit tests |
| **Risk** | **Medium** if CI and image build are decoupled |
| **Fix complexity** | **Low** — CI builds image after verify, or remove skip in multi-stage |
| **Recommended solution** | Build image only from CI artifact that passed `mvn verify` |

---

### TD-M10 — `email_verified` not enforced

| Field | Detail |
|-------|--------|
| **Description** | `User.emailVerified` defaults `false` on register; no verification flow; login allowed regardless. |
| **Location** | `AuthService.register()`, `User.java` |
| **System impact** | Unverified emails can use full product |
| **Risk** | **Medium** |
| **Fix complexity** | **Medium** — email verification flow |
| **Recommended solution** | Optional for MVP; enforce before production marketing launch |

---

### TD-M11 — Schedulers assume single backend instance

| Field | Detail |
|-------|--------|
| **Description** | `DailyTaskScheduler` (07:30) and `NotificationScheduler` (08:00) run in-process with no distributed lock. |
| **Location** | `DailyTaskScheduler.java`, `NotificationScheduler.java` |
| **System impact** | Horizontal scale → duplicate tasks/notifications |
| **Risk** | **Medium** when replicas > 1 |
| **Fix complexity** | **Medium** — ShedLock or DB advisory lock |
| **Recommended solution** | ShedLock on scheduler methods before multi-instance deploy |

---

### TD-M12 — CI without dependency security scanning

| Field | Detail |
|-------|--------|
| **Description** | No Dependabot, OWASP dependency-check, or `npm audit` in workflows. |
| **Location** | `.github/workflows/*` — no security jobs |
| **System impact** | Vulnerable dependencies may go unnoticed |
| **Risk** | **Medium** |
| **Fix complexity** | **Low** |
| **Recommended solution** | Dependabot + `npm audit` / OWASP in CI (non-blocking then blocking) |

---

### TD-M13 — Bank integrations feature flag off, UI mock-only

| Field | Detail |
|-------|--------|
| **Description** | `flowiq.features.bank-integrations-enabled=false`; integrations page redirects to coming-soon; data from `mock-data`. |
| **Location** | `application.properties`; `IntegrationsView.tsx`; `app/integrations/page.tsx` |
| **System impact** | CSV import only path for real bank data |
| **Risk** | **Medium** — product scope clarity |
| **Fix complexity** | **High** when implemented |
| **Recommended solution** | ADR-011 when Phase 1 starts |

---

### TD-M14 — Settings profile/notifications/security sections non-functional

| Field | Detail |
|-------|--------|
| **Description** | Beyond regional/theme, settings cards (profile, notifications, security) are display-only placeholders. |
| **Location** | `SettingsView.tsx` lines 103–120 |
| **System impact** | User expectation mismatch |
| **Risk** | **Medium** |
| **Fix complexity** | **Medium** — tied to TD-H01 |
| **Recommended solution** | Hide incomplete sections or implement with settings API |

---

### TD-M15 — No structured application logging / correlation IDs

| Field | Detail |
|-------|--------|
| **Description** | Standard SLF4J logging only; no MDC request ID filter. |
| **Location** | Various `@Slf4j` classes; no `RequestIdFilter` |
| **System impact** | Hard to trace request across services in logs |
| **Risk** | **Medium** |
| **Fix complexity** | **Low** |
| **Recommended solution** | `OncePerRequestFilter` setting `X-Request-Id` / MDC |

---

### TD-M16 — `FlowiqBackendApplicationTests` requires external PostgreSQL

| Field | Detail |
|-------|--------|
| **Description** | `@SpringBootTest` context load test expects real DB + Flyway; fails in CI without Postgres (may be skipped or fail depending on env). |
| **Location** | `FlowiqBackendApplicationTests.java` |
| **System impact** | CI reliability; false sense of integration coverage |
| **Risk** | **Medium** |
| **Fix complexity** | **Medium** — Testcontainers or `@SpringBootTest` with test profile |
| **Recommended solution** | Testcontainers in test profile; or move to dedicated IT class |

---

## Low

### TD-L01 — ADR gaps (lower priority)

| Field | Detail |
|-------|--------|
| **Description** | Missing ADRs: monolith (010), chat strategy (012), scheduling (015), report stack (016), Docker build (018), demo user (021), CORS (022), i18n headers (020). |
| **Location** | `ADR_COVERAGE_REPORT.md` |
| **System impact** | Documentation only |
| **Risk** | **Low** |
| **Fix complexity** | **Low** |
| **Recommended solution** | Add when touching each area |

---

### TD-L02 — No TanStack Query / global server cache

| Field | Detail |
|-------|--------|
| **Description** | Feature hooks refetch on every navigation; duplicate API calls. |
| **Location** | `useForecasts.ts`, `useAnalytics.ts`, etc. |
| **System impact** | Extra load, slower UX |
| **Risk** | **Low** at MVP scale |
| **Fix complexity** | **Medium** |
| **Recommended solution** | Add React Query when module count or traffic grows |

---

### TD-L03 — Stale `services/README.md` references mock dashboard

| Field | Detail |
|-------|--------|
| **Description** | Frontend services README still documents mock-data paths for dashboard. |
| **Location** | `flowiq-frontend/src/services/README.md` |
| **System impact** | Developer confusion |
| **Risk** | **Low** |
| **Fix complexity** | **Low** |
| **Recommended solution** | Update README to reflect API-only dashboard |

---

### TD-L04 — `logout` endpoint is no-op on server

| Field | Detail |
|-------|--------|
| **Description** | `POST /api/auth/logout` returns 204 without invalidating token. |
| **Location** | `AuthController.logout()` |
| **System impact** | Documented client-side logout only |
| **Risk** | **Low** with short TTL; **Medium** with 24h access |
| **Fix complexity** | **Medium** with denylist |
| **Recommended solution** | Document; implement token blocklist with refresh flow |

---

### TD-L05 — JSONB not used in schema

| Field | Detail |
|-------|--------|
| **Description** | ADR-004 mentions JSONB future use; V1–V5 use relational columns only. |
| **Location** | Migrations V1–V5 |
| **System impact** | None today |
| **Risk** | **Low** |
| **Fix complexity** | **Low** when needed |
| **Recommended solution** | Add JSONB when import error details or notification metadata need flexibility |

---

### TD-L06 — No connection pool tuning documented in code

| Field | Detail |
|-------|--------|
| **Description** | Default HikariCP settings only in `application.properties`. |
| **Location** | `application.properties` |
| **System impact** | May underperform under load |
| **Risk** | **Low** at MVP traffic |
| **Fix complexity** | **Low** |
| **Recommended solution** | Document pool size for prod based on load test |

---

### TD-L07 — Ukrainian FOP checker runs client-side only

| Field | Detail |
|-------|--------|
| **Description** | `checker/engine/eligibility-engine.ts` — no backend validation of eligibility logic. |
| **Location** | `flowiq-frontend/src/features/business-guide/checker/` |
| **System impact** | Logic not shared with API; can't audit checker results |
| **Risk** | **Low** for MVP tool |
| **Fix complexity** | **Medium** |
| **Recommended solution** | Optional backend mirror if checker becomes compliance-critical |

---

### TD-L08 — Report/import async job error visibility

| Field | Detail |
|-------|--------|
| **Description** | `import_jobs` and `report_jobs` exist; limited user-facing error detail structure. |
| **Location** | `V1__initial_schema.sql`; `ImportService`, `ReportsService` |
| **System impact** | Users may get generic failure messages |
| **Risk** | **Low** |
| **Fix complexity** | **Medium** |
| **Recommended solution** | JSONB `error_details` on job tables |

---

### TD-L09 — No `application-docker.properties` profile activation documented in CI

| Field | Detail |
|-------|--------|
| **Description** | Docker-specific JDBC URL in separate file; CI does not run integration against Docker profile. |
| **Location** | `application-docker.properties` |
| **System impact** | Minor env parity gap |
| **Risk** | **Low** |
| **Fix complexity** | **Low** |
| **Recommended solution** | Document `SPRING_PROFILES_ACTIVE=docker` for container runs |

---

### TD-L10 — Frontend ESLint only in CI (no type-check job)

| Field | Detail |
|-------|--------|
| **Description** | `frontend-ci.yml` runs lint + build; `tsc --noEmit` not separate. |
| **Location** | `.github/workflows/frontend-ci.yml` |
| **System impact** | Build may surface type errors late |
| **Risk** | **Low** — `next build` includes type check |
| **Fix complexity** | **Low** |
| **Recommended solution** | Optional explicit `tsc` step for faster feedback |

---

### TD-L11 — CORS allowlist hardcoded

| Field | Detail |
|-------|--------|
| **Description** | Origins in `CorsConfig.java`; new preview URLs require code change. |
| **Location** | `CorsConfig.java` |
| **System impact** | Friction for preview deployments |
| **Risk** | **Low** |
| **Fix complexity** | **Low** |
| **Recommended solution** | `flowiq.cors.allowed-origins` from env |

---

### TD-L12 — JaCoCo coverage not enforced in CI

| Field | Detail |
|-------|--------|
| **Description** | CI uploads JaCoCo artifact but no minimum coverage gate. |
| **Location** | `backend-ci.yml`, `pom.xml` jacoco plugin |
| **System impact** | Coverage can regress silently |
| **Risk** | **Low** |
| **Fix complexity** | **Low** |
| **Recommended solution** | `jacoco:check` with 60% line minimum on engines/services |

---

## Focus Area Verification (Requested)

| Area | Status in code | Register IDs |
|------|----------------|--------------|
| **Audit Log** | ❌ Not implemented | TD-C02, TD-H14 |
| **User Settings backend** | ❌ Not implemented | TD-H01, TD-M14 |
| **Refresh Token endpoint** | ❌ Not implemented | TD-C06, TD-H02 |
| **Frontend tests** | ❌ Zero test files | TD-H06 |
| **E2E tests** | ❌ No Playwright/Cypress | TD-H07 |
| **Staging environment** | ❌ TBD in docs only | TD-H04 |
| **CD pipeline** | ❌ CI only, no deploy | TD-H05 |
| **Observability** | ❌ No actuator/tracing | TD-H08, TD-M15 |
| **Monitoring** | ❌ No APM/alerting in repo | TD-H09 |
| **Security gaps** | ⚠️ Partial JWT, no RBAC, Swagger public, demo user | TD-C03, TD-C06, TD-H03, TD-H10, TD-H12, TD-H13 |
| **ADR gaps** | ⚠️ 14+ decisions undocumented | TD-H14, TD-L01 |

---

## Prioritized 3-Month Roadmap

### Month 1 — Production blockers & security baseline

**Goal:** Make production deploy **defensible** for architect and security review.

| Week | Deliverable | Debt items |
|------|-------------|------------|
| 1 | `application-prod.properties`; env-based `jwt.secret`; disable `show-sql` in prod | TD-C03 |
| 1 | `flowiq.features.demo-seed-enabled`; disable seed + `DemoUserSeedService` in prod | TD-C01, TD-H12 |
| 2 | `POST /api/auth/refresh` + frontend axios retry | TD-C06, TD-H02 |
| 2 | Restrict Swagger to dev profile | TD-H13 |
| 3 | `transactions.source` column + seed/import/manual labeling | TD-C04 |
| 4 | ADR-009 draft + `TaxConfigurationService` extract (single module) | TD-C05 |
| 4 | ADR-013 + `audit_log` schema V6 (write path for auth + transaction CRUD) | TD-C02 |

**Exit criteria:** No default secrets in prod; seed off; refresh works; audit log writes; tax constants in one place.

---

### Month 2 — Quality, settings, staging, observability

**Goal:** Testable releases and operable staging.

| Week | Deliverable | Debt items |
|------|-------------|------------|
| 5 | Testcontainers in CI + 3 integration tests (Flyway, `TransactionRepository`, auth) | TD-H11, TD-M16 |
| 6 | Vitest + 10 frontend unit tests (auth, api, one feature hook) | TD-H06 |
| 7 | `GET/PATCH /api/settings`; wire regional prefs; hide or stub profile sections | TD-H01, TD-M14 |
| 7 | ADR-017 + `@PreAuthorize` on admin paths; verify `user_id` on all mutations | TD-H03 |
| 8 | Staging environment (managed Postgres + backend deploy + Vercel preview) | TD-H04 |
| 8 | Spring Actuator + request correlation ID + health probe docs | TD-H08, TD-H09, TD-M15 |

**Exit criteria:** CI runs against real Postgres; staging URL exists; settings persist server-side; `/actuator/health` monitored.

---

### Month 3 — E2E, CD, consolidation

**Goal:** Repeatable delivery and reduced architectural drift.

| Week | Deliverable | Debt items |
|------|-------------|------------|
| 9 | Playwright smoke E2E (login, dashboard, import) in CI | TD-H07 |
| 10 | CD workflow: deploy to staging on merge; manual prod promotion | TD-H05, ADR-019 |
| 11 | Unify AI Accountant forecasts with `ForecastService` | TD-M03 |
| 11 | Unify or document health scores | TD-M04 |
| 12 | Remove or wire dead AI hooks (`AnalyticsInsightProvider`, `TransactionInsightService`) | TD-M01, TD-M02 |
| 12 | ADR-013/017/019 accepted; Dependabot enabled | TD-H14, TD-M12, TD-L01 |

**Exit criteria:** E2E green on staging; one-click staging deploy; forecast/health consistency addressed; ADR backlog for prod decisions closed.

---

## How to Maintain This Register

1. Add row `TD-{severity}{nn}` when new debt is discovered in PR review.
2. Close items with PR link and date when resolved.
3. Re-audit quarterly or before major release.
4. Sync with [ARCHITECT_REVIEW_CHEAT_SHEET.md](ARCHITECT_REVIEW_CHEAT_SHEET.md) and [ADR_COVERAGE_REPORT.md](adr/ADR_COVERAGE_REPORT.md).

---

## Related Documents

| Document | Link |
|----------|------|
| Architect cheat sheet | [ARCHITECT_REVIEW_CHEAT_SHEET.md](ARCHITECT_REVIEW_CHEAT_SHEET.md) |
| ADR defense | [ADR_DEFENSE_GUIDE.md](ADR_DEFENSE_GUIDE.md) |
| AI code audit | [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md) |
| Environments | [../deployment/environments.md](../deployment/environments.md) |
| Authorization gaps | [../security/authorization.md](../security/authorization.md) |

**Prepared:** 2026-06-23
