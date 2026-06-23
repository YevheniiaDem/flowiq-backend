# Architect Trap Questions — FlowIQ

**Audit date:** 2026-06-23  
**Auditor lens:** Picky principal / security architect review  
**Source of truth:** `flowiq-backend`, `flowiq-frontend` code  
**Companion:** [ARCHITECT_INTERVIEW_GUIDE.md](ARCHITECT_INTERVIEW_GUIDE.md), [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md), [REQUEST_FLOW_MAP.md](REQUEST_FLOW_MAP.md)

---

## How to use

Each **trap** is something a strict architect is likely to probe in code review or Q&A. For every item:

| Field | Purpose |
|-------|---------|
| **Why they care** | What triggers scrutiny |
| **Severity** | Critical / High / Medium / Low |
| **How to answer** | Honest, defensible talk track (MVP vs production) |
| **Remediation** | Concrete fix plan |

**Severity scale**

| Level | Meaning |
|-------|---------|
| **Critical** | Blocks production trust, security, or compliance |
| **High** | Must be on roadmap before scale or external audit |
| **Medium** | Technical debt; acceptable in MVP with documented plan |
| **Low** | Smell or polish; fix when touching the area |

---

## Summary heatmap

| Category | Critical | High | Medium | Low |
|----------|----------|------|--------|-----|
| Architectural anti-patterns | 2 | 4 | 5 | 1 |
| Code smells | 1 | 3 | 6 | 3 |
| Security smells | 3 | 5 | 2 | 1 |
| Scalability | 0 | 3 | 4 | 1 |
| Maintainability | 1 | 4 | 5 | 2 |
| Suspicious design | 2 | 3 | 4 | 2 |

---

# 1. Architectural anti-patterns

### T-A01 — Synthetic data indistinguishable from real data

| | |
|--|--|
| **Evidence** | `TransactionSeedService.seedIfEmpty()` called from 7+ services; no `transactions.source` column (V1–V5) |
| **Code** | `src/main/java/com/flowiq/service/TransactionSeedService.java`; callers in `DashboardService`, `AnalyticsService`, `ForecastService`, `ReportsService`, `ChatService`, `AIAccountantService`, `TaskService` |
| **ADR** | [002-transaction-seed-strategy.md](adr/002-transaction-seed-strategy.md) — accepted for demo |

**Why they care:** Financial/tax dashboards show plausible revenue on first login without user action. Architect will ask: *“How does the user know this isn’t their real business?”*

**Severity:** **Critical**

**How to answer:** “Accepted for MVP demo onboarding per ADR-002. We document it in [data-sources.md](data-sources.md) and debt register TD-C01/C04. Production plan: profile-gated seed + `source` column + UI banner. Not hidden — acknowledged gap.”

**Remediation:** `flowiq.features.demo-seed-enabled=false` in prod profile; Flyway `V6` add `transactions.source`; API field for UI badge; update ADR-002 with production disable criteria.

---

### T-A02 — “Pluggable AI” with zero LLM implementations

| | |
|--|--|
| **Evidence** | `AIInsightProvider`, `AnalyticsInsightProvider`, `CategorizationProvider` — interfaces wired, **no `@Component` implementations**; no LLM SDK in `pom.xml` |
| **Code** | `AIAccountantService` (empty provider loop); `AnalyticsService` (provider never called); `CategorizationEngine` |
| **ADR** | [001-pluggable-ai-providers.md](adr/001-pluggable-ai-providers.md) |

**Why they care:** Looks like **speculative generality** — abstraction tax without second implementation. *“Why not YAGNI until OpenAI ships?”*

**Severity:** **Medium** (High if marketed as “AI product” without disclaimer)

**How to answer:** “ADR-001 is forward-compatible extension point. Production intelligence is **rule-based** and documented in [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md). Interfaces cost little; first LLM bean plugs in without controller changes.”

**Remediation:** ADR-001 add “MVP: rule-only”; remove or use `TransactionInsightService`; add `NoOpAnalyticsInsightProvider` only if needed for tests; UI copy: “Automated insights (rule-based)”.

---

### T-A03 — Dual forecast implementations

| | |
|--|--|
| **Evidence** | `ForecastService` + `ForecastEngine` vs `AIAccountantService.buildForecast()` inline — same product concept, different math |
| **Code** | `forecasts/service/ForecastService.java`, `service/AIAccountantService.java` |

**Why they care:** **Duplicate bounded context** — metrics can diverge between Forecast Center and AI Accountant. Classic *anemic duplication* anti-pattern.

**Severity:** **High**

**How to answer:** “Known inconsistency, logged in AI audit. Forecast Center is canonical (`ForecastEngine`). AI Accountant forecasts are MVP shortcut. Consolidation is on roadmap.”

**Remediation:** Delegate `AIAccountantService.getForecasts()` to `ForecastService` or shared `ForecastProjectionService`; contract test asserting same inputs → same outputs.

---

### T-A04 — Cross-domain coupling: notifications → tasks

| | |
|--|--|
| **Evidence** | `NotificationRuleEngine` imports `TaskGeneratorService`, `TaskType`, `TaskPriority` |
| **Code** | `notifications/service/NotificationRuleEngine.java` |

**Why they care:** Violates **bounded context** — notification domain creates tasks. Harder to extract microservices or test in isolation.

**Severity:** **Medium**

**How to answer:** “Monolith MVP — compliance alerts often spawn tasks (e.g. file declaration). We accept in-process coupling; events would come with service split.”

**Remediation:** Domain events (`ComplianceAlertRaised` → task handler); or `ComplianceOrchestrator` above both modules.

---

### T-A05 — BYTEA report storage in OLTP database

| | |
|--|--|
| **Evidence** | `report_jobs.file_content BYTEA`; synchronous generate in HTTP request |
| **Code** | `entity/ReportJob.java`, `service/ReportsService.java`, `reports/ReportFileGenerator.java` |

**Why they care:** **Blob-in-Postgres** bloats backups, connection memory, and blocks async scaling.

**Severity:** **High** (at scale); **Medium** (MVP)

**How to answer:** “MVP stores PDF/XLSX in DB for simplicity — documented in [production-deployment.md](../deployment/production-deployment.md). Migration to S3/GCS is planned before high volume.”

**Remediation:** Object storage + `file_url` column; async job queue (`@Async` or SQS); keep metadata in `report_jobs`.

---

### T-A06 — Schedulers without leader election

| | |
|--|--|
| **Evidence** | `@Scheduled` on every instance: `DailyTaskScheduler` (07:30), `NotificationScheduler` (08:00) |
| **Code** | `tasks/scheduler/DailyTaskScheduler.java`, `notifications/scheduler/NotificationScheduler.java` |

**Why they care:** Horizontal scale → **duplicate tasks/notifications** per user per day.

**Severity:** **High** (multi-instance); **Low** (single pod MVP)

**How to answer:** “Current deployment assumption is single backend instance. Documented scalability gap. ShedLock or K8s CronJob before second replica.”

**Remediation:** ShedLock table + `@SchedulerLock`; or external scheduler (K8s CronJob hitting admin API).

---

### T-A07 — Feature flag defined but unused

| | |
|--|--|
| **Evidence** | `FeatureFlags.bankIntegrationsEnabled` — `@EnableConfigurationProperties`, **never injected** in import/bank code |
| **Code** | `config/FeatureFlags.java`, `application.properties` |

**Why they care:** **Configuration theater** — suggests control that doesn’t exist.

**Severity:** **Low**

**How to answer:** “Flag prepared for bank roadmap; import remains CSV-only. Wiring deferred until Monobank/Privat API phase.”

**Remediation:** Inject flag in `ImportController` or remove property until Phase 2; document in ADR-011.

---

### T-A08 — Accepted ADR vs production safety tension

| | |
|--|--|
| **Evidence** | ADR-002 **accepts** auto-seed; production checklist says disable demo — governance mismatch |
| **ADR** | 002 vs [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) TD-C01 |

**Why they care:** *“You accepted an anti-pattern as architecture — how do you sunset it?”*

**Severity:** **High** (governance)

**How to answer:** “ADR-002 scoped demo/MVP. We’re adding ADR-009/013 and prod criteria in debt register. ADR status can move to ‘Superseded for production’ once flag ships.”

**Remediation:** ADR-002 amendment: production requires `demo-seed-enabled=false`; link from ADR index.

---

### T-A09 — Global knowledge base in tenant app

| | |
|--|--|
| **Evidence** | `knowledge_articles` has no `user_id`; Flyway V5 seeds global content |
| **Code** | `knowledge/entity/KnowledgeArticle.java`, `V5__create_knowledge_articles_table.sql` |

**Why they care:** Fine for CMS-style content; trap if architect expects **per-tenant customization** or PII in articles.

**Severity:** **Low** (by design)

**How to answer:** “Business Guide is curated regulatory content, not user data. Tenant-specific notes would be a separate table later.”

**Remediation:** Document in data-sources; optional `tenant_id` nullable for future white-label.

---

### T-A10 — Presentation orchestration in `DashboardController`

| | |
|--|--|
| **Evidence** | One controller injects **4 services** for different widgets |
| **Code** | `controller/DashboardController.java` |

**Why they care:** May look like **BFF aggregation** without dedicated facade — coupling UI shape to controller.

**Severity:** **Low**

**How to answer:** “Thin controller — each endpoint delegates to one service. BFF/facade (`DashboardFacade`) optional if widget bundle API needed.”

**Remediation:** Optional `DashboardFacade` if composite endpoint count grows; keep current for MVP.

---

# 2. Code smells

### T-C01 — `getCurrentUserEntity()` duplicated ~10 times

| | |
|--|--|
| **Evidence** | Private copy in `DashboardService`, `TransactionService`, `AnalyticsService`, `ForecastService`, `ReportsService`, `ChatService`, `ImportService`, `TaskService`, `AIAccountantService`, `NotificationService` |
| **Code** | Grep `private User getCurrentUserEntity` under `src/main/java` |

**Why they care:** **DRY violation** — bug fix in auth resolution must be repeated; easy to miss one service.

**Severity:** **Medium**

**How to answer:** “Layered MVP — each service resolves user from `SecurityContextHolder`. Extraction to `CurrentUserService` is planned.”

**Remediation:** `CurrentUserService` or `@ControllerAdvice` request attribute; or Spring `@AuthenticationPrincipal UserPrincipal` method injection in controllers only + pass `userId` to services.

---

### T-C02 — Dead Spring bean: `TransactionInsightService`

| | |
|--|--|
| **Evidence** | `@Service` registered, **zero callers** |
| **Code** | `service/TransactionInsightService.java` |

**Why they care:** **Dead code in classpath** — confusing for onboarding and architecture diagrams.

**Severity:** **Medium**

**How to answer:** “Future hook for LLM context per ADR-001. Documented as unused in AI audit. Remove or wire before next release.”

**Remediation:** Delete until needed, or wire `DashboardService.getInsights()` to it; mark `@Profile("!prod")` if experimental.

---

### T-C03 — Injected but never called: `AnalyticsInsightProvider`

| | |
|--|--|
| **Evidence** | Constructor injection in `AnalyticsService`; no method invokes list |
| **Code** | `service/AnalyticsService.java`, `analytics/AnalyticsInsightProvider.java` |

**Why they care:** **Incomplete refactor** smell — looks half-done.

**Severity:** **Medium**

**How to answer:** “Provider slot for LLM narratives. Not invoked in rule-based MVP. Same pattern as ADR-001.”

**Remediation:** Remove field until implementation exists, or call in `getFopInsights()` behind feature flag.

---

### T-C04 — Silent exception swallowing in JWT filter

| | |
|--|--|
| **Evidence** | `catch (Exception ignored)` clears context, continues chain |
| **Code** | `security/JwtAuthenticationFilter.java:60` |

**Why they care:** **Fail-open debugging nightmare** — invalid tokens look like anonymous; no audit trail.

**Severity:** **High**

**How to answer:** “Invalid tokens are treated as unauthenticated; protected routes still 401. We should log at DEBUG/WARN with correlation ID — acknowledged gap.”

**Remediation:** Log structured warning (no token payload); optional `MDC` request ID; never log full JWT.

---

### T-C05 — `log.info` with plaintext demo password

| | |
|--|--|
| **Evidence** | `DemoUserSeedService` logs `DEMO_EMAIL / DEMO_PASSWORD` |
| **Code** | `service/DemoUserSeedService.java:41` |

**Why they care:** **Credential exposure** in log aggregation.

**Severity:** **High**

**How to answer:** “Dev/demo bootstrap only. Should not run in prod — we’ll gate by profile and remove password from logs.”

**Remediation:** `@Profile("!prod")`; log email only; rotate demo password via env.

---

### T-C06 — Odd JWT key derivation

| | |
|--|--|
| **Evidence** | `Base64.encode(secretKey.getBytes())` then `Base64.decode` — non-standard double step |
| **Code** | `security/JwtService.java:96-100` |

**Why they care:** *“Is the signing key strength predictable?”* — crypto review trigger.

**Severity:** **Medium**

**How to answer:** “Uses JJWT `Keys.hmacShaKeyFor` with derived bytes. We’ll move to env-provided Base64 secret (256+ bit) per security checklist.”

**Remediation:** `jwt.secret` as proper Base64 key in prod; `validateKeyLength()` on startup; document in ADR-006.

---

### T-C07 — Surefire excludes `*Tests.java`

| | |
|--|--|
| **Evidence** | Only `**/*Test.java` in CI; `FlowiqBackendApplicationTests` excluded |
| **Code** | `pom.xml` surefire includes |

**Why they care:** **False confidence** — context load test not in gate.

**Severity:** **Medium**

**How to answer:** “Naming convention separates fast unit tests from context test. We can rename or add second surefire execution for integration.”

**Remediation:** Rename to `*Test.java` or add `integration-test` profile with Testcontainers.

---

### T-C08 — `GlobalExceptionHandler` on generic `Exception`

| | |
|--|--|
| **Evidence** | Catch-all may return 500 with `ex.getMessage()` |
| **Code** | `exception/GlobalExceptionHandler.java` |

**Why they care:** **Information leakage** if internal messages reach client.

**Severity:** **Medium**

**How to answer:** “Custom exceptions mapped; generic handler returns safe message in prod. Verify `application-prod` masks internals.”

**Remediation:** Prod profile: generic “Internal error” + log full stack server-side only.

---

### T-C09 — Package layout inconsistency

| | |
|--|--|
| **Evidence** | Controllers in `com.flowiq.controller` vs `tasks.controller`, `forecasts.controller`, `knowledge.controller` |
| **Code** | Package tree under `src/main/java/com/flowiq` |

**Why they care:** **Modular monolith** hygiene — harder to enforce module boundaries.

**Severity:** **Low**

**How to answer:** “Early modules (tasks, forecasts, knowledge) got subpackages; core CRUD stayed flat. Align incrementally or document as ‘core vs feature modules’.”

**Remediation:** ADR-010 monolith modules map; optional move to `com.flowiq.{module}.api`.

---

### T-C10 — `emailVerified` never enforced

| | |
|--|--|
| **Evidence** | Set `false` on register; never checked on login or API |
| **Code** | `entity/User.java`, `service/AuthService.java` |

**Why they care:** Field implies **verification workflow** that doesn’t exist.

**Severity:** **Low**

**How to answer:** “Schema prepared for future email verification. Not in MVP scope.”

**Remediation:** Remove from API exposure until implemented, or block login if `!emailVerified` when flow exists.

---

### T-C11 — Logout is no-op on server

| | |
|--|--|
| **Evidence** | `AuthController.logout()` → 204, no token invalidation |
| **Code** | `controller/AuthController.java`, `service/AuthService` |

**Why they care:** **Misleading API contract** — “logout” doesn’t revoke JWT.

**Severity:** **Medium**

**How to answer:** “Stateless JWT — logout is client-side token discard. Documented in [jwt-flow.md](../security/jwt-flow.md). Refresh/revocation is roadmap.”

**Remediation:** Document OpenAPI accurately; implement refresh rotation + denylist for stolen-token scenario.

---

# 3. Security smells

### T-S01 — JWT in `localStorage`

| | |
|--|--|
| **Evidence** | `auth.service.ts`, `api.ts` interceptors |
| **Code** | `flowiq-frontend/src/services/auth.service.ts`, `api.ts` |

**Why they care:** **XSS → token theft** — standard OWASP review question.

**Severity:** **High**

**How to answer:** “MVP SPA pattern. Mitigations: CSP, sanitize inputs, short access TTL. Production option: HttpOnly Secure cookies + SameSite.”

**Remediation:** Cookie-based session or BFF; CSP headers on Vercel; access token 15–60 min when refresh works.

---

### T-S02 — Refresh token issued, no `/auth/refresh`

| | |
|--|--|
| **Evidence** | `JwtService.generateRefreshToken`; no endpoint; frontend stores unused `refreshToken` |
| **Code** | `AuthController.java`, `flowiq-frontend/src/services/api.ts` |

**Why they care:** **Half-implemented OAuth-style flow** — refresh in localStorage with no rotation = worst of both worlds.

**Severity:** **Critical** (if claiming production auth); **High** (MVP)

**How to answer:** “ADR-006 Phase 1 ships access + refresh generation. Phase 2 endpoint planned. Frontend doesn’t call refresh yet — clears on 401.”

**Remediation:** `POST /api/auth/refresh`; validate `type=refresh`; axios interceptor; shorten access TTL.

---

### T-S03 — Secrets in committed `application.properties`

| | |
|--|--|
| **Evidence** | `jwt.secret`, DB password, `show-sql=true` |
| **Code** | `src/main/resources/application.properties` |

**Why they care:** **Instant fail** on security checklist / pen test.

**Severity:** **Critical**

**How to answer:** “Dev defaults for local Docker Compose only. Production requires env injection — documented, not yet enforced by startup guard.”

**Remediation:** `application-prod.properties`; `${JWT_SECRET}` required; fail fast if default secret; disable `show-sql`.

---

### T-S04 — Demo user with known password in all environments

| | |
|--|--|
| **Evidence** | `demo@flowiq.ai` / `demo123` via `ApplicationRunner` |
| **Code** | `DemoUserSeedService.java` |

**Why they care:** **Backdoor account** if enabled in staging/prod.

**Severity:** **Critical** (prod); **Medium** (dev)

**How to answer:** “Demo user for local QA and automation (`flowiq-automation` local.properties). Must be `@Profile('local')` only — gap acknowledged.”

**Remediation:** Profile-gate; remove from prod/staging; use secrets for E2E users.

---

### T-S05 — RBAC roles without enforcement

| | |
|--|--|
| **Evidence** | `User.Role`: ADMIN, USER, VIEWER; no `@PreAuthorize`; `SecurityConfig` only `authenticated()` |
| **Code** | `entity/User.java`, `config/SecurityConfig.java` |

**Why they care:** **Security theater** — role in JWT implies authorization that doesn’t exist.

**Severity:** **High**

**How to answer:** “All users have same API access today; `user_id` scoping in services. Roles reserved for admin console (future). Documented in [authorization.md](../security/authorization.md).”

**Remediation:** ADR-017; remove VIEWER from JWT until used, or enforce read-only endpoints.

---

### T-S06 — Client-side-only route protection

| | |
|--|--|
| **Evidence** | `MainLayout` checks `authService.isAuthenticated()` (token presence); no Next.js middleware |
| **Code** | `flowiq-frontend/src/shared/components/layout/MainLayout.tsx` |

**Why they care:** **UX security** — URLs flash before redirect; not true defense (API still protected).

**Severity:** **Medium**

**How to answer:** “Backend enforces JWT on all mutations. Frontend guard is UX. Middleware optional for SSR routes.”

**Remediation:** `middleware.ts` cookie check; or accept API-only security model explicitly.

---

### T-S07 — CORS allowlist hardcoded

| | |
|--|--|
| **Evidence** | Origins in Java `CorsConfig` — new prod URL needs code change |
| **Code** | `config/CorsConfig.java` |

**Why they care:** **Ops friction** and risk of `*` temptation under pressure.

**Severity:** **Medium**

**How to answer:** “Known origins including Vercel preview. Env-based list planned for prod.”

**Remediation:** `flowiq.cors.allowed-origins` property; validate non-empty in prod.

---

### T-S08 — No rate limiting on `/auth/register` and `/login`

| | |
|--|--|
| **Evidence** | Public endpoints; no Bucket4j / gateway throttle in code |
| **Code** | `SecurityConfig.java`, `AuthController.java` |

**Why they care:** **Brute force and spam registration.**

**Severity:** **High**

**How to answer:** “MVP relies on infra (Cloudflare, API gateway) — not in app yet. On roadmap for prod.”

**Remediation:** Rate limit filter or reverse proxy; CAPTCHA on register if abused.

---

### T-S09 — No audit trail for financial / tax advice

| | |
|--|--|
| **Evidence** | No `audit_log` table or service |
| **Code** | Absent from migrations V1–V5 |

**Why they care:** **Regulatory and incident response** for fintech.

**Severity:** **Critical**

**How to answer:** “Acknowledged TD-C02. MVP focuses on features; audit log is pre-production gate with ADR-013.”

**Remediation:** `AuditLogService` on transaction CRUD, import, report generate, AI chat; immutable store.

---

### T-S10 — Swagger public in default config

| | |
|--|--|
| **Evidence** | `/swagger-ui/**` permitAll |
| **Code** | `SecurityConfig.java` |

**Why they care:** **Attack surface enumeration** in production.

**Severity:** **Medium** (prod)

**How to answer:** “Dev DX for MVP. Prod profile will disable or protect Swagger.”

**Remediation:** `@Profile("!prod")` on OpenAPI or basic auth on `/swagger-ui`.

---

# 4. Scalability bottlenecks

### T-X01 — `userRepository.findAll()` in schedulers

| | |
|--|--|
| **Evidence** | Both schedulers load **all users** into memory |
| **Code** | `NotificationScheduler.java`, `DailyTaskScheduler.java` |

**Why they care:** O(users) memory and query — breaks at 100k+ users.

**Severity:** **High** (scale)

**How to answer:** “MVP user count small. Pagination or ‘active users since’ query before scale.”

**Remediation:** `findByActiveTrue(Pageable)`; batch processing; cursor-based iteration.

---

### T-X02 — Synchronous CSV import and report generation

| | |
|--|--|
| **Evidence** | `ImportService.upload`, `ReportsService.generate` in request thread; 10MB multipart |
| **Code** | `ImportService.java`, `ReportsService.java`, `application.properties` |

**Why they care:** **Thread pool exhaustion** under concurrent uploads.

**Severity:** **High**

**How to answer:** “10MB cap limits blast radius. Async jobs when p95 latency matters.”

**Remediation:** `@Async` + job status polling; or message queue workers.

---

### T-X03 — No caching layer

| | |
|--|--|
| **Evidence** | No `@Cacheable`, Redis, or CDN for API reads |
| **Code** | `pom.xml` — no cache starter |

**Why they care:** Dashboard hits DB on every widget load.

**Severity:** **Medium**

**How to answer:** “Low traffic MVP. User-scoped cache or materialized dashboard snapshot later.”

**Remediation:** Caffeine cache per `userId` for dashboard stats; TTL 1–5 min.

---

### T-X04 — N+1 risk on chat conversations

| | |
|--|--|
| **Evidence** | `ChatConversation` → `List<ChatMessage>` likely lazy load on list |
| **Code** | `entity/ChatConversation.java`, `ChatService.getConversations()` |

**Why they care:** List endpoint may trigger **N+1 queries**.

**Severity:** **Medium**

**How to answer:** “Verify with Hibernate stats / integration test. `@EntityGraph` or DTO projection if needed.”

**Remediation:** `JOIN FETCH` query in repository; pagination on messages.

---

### T-X05 — Missing FK on high-volume child tables

| | |
|--|--|
| **Evidence** | `notifications`, `import_jobs`, `report_jobs` — `user_id` without FK |
| **Code** | `V3__create_notifications_table.sql`, V1 import/report |

**Why they care:** Orphan rows; harder **cascade delete** for GDPR erasure.

**Severity:** **Medium**

**How to answer:** “Indexed for queries; user delete API not implemented yet. FK added in V6 with ON DELETE CASCADE policy.”

**Remediation:** Flyway V6 FKs + data cleanup; document erasure procedure.

---

### T-X06 — Single PostgreSQL for blobs + OLTP

| | |
|--|--|
| **Evidence** | Reports BYTEA + transactions in same DB |
| **Code** | `report_jobs` table |

**Why they care:** Backup size and IOPS contention.

**Severity:** **Medium** (see T-A05)

**How to answer:** Same as T-A05.

**Remediation:** Object storage offload.

---

### T-X07 — No connection pool tuning documented

| | |
|--|--|
| **Evidence** | Default Hikari settings in Spring Boot |
| **Code** | `application.properties` |

**Why they care:** Under load, **pool starvation** vs DB max connections.

**Severity:** **Low**

**How to answer:** “Defaults fine for MVP. Tune with load test before launch.”

**Remediation:** Document `maximum-pool-size`; align with PG `max_connections`.

---

# 5. Maintainability issues

### T-M01 — FOP/tax constants duplicated 4+ times (+ frontend)

| | |
|--|--|
| **Evidence** | `INCOME_LIMITS`, `ESV_MONTHLY`, `SINGLE_TAX_RATES` in `AnalyticsService`, `ForecastService`, `NotificationRuleEngine`, `TaskRuleEngine`; frontend `eligibility-engine.ts`, `business-guide.service.ts` |
| **Code** | Grep `INCOME_LIMITS` under backend and frontend |

**Why they care:** Law change = **multi-repo surgery**; inconsistent advice between modules.

**Severity:** **Critical** (tax accuracy)

**How to answer:** “TD-C05. Single `TaxConfigurationService` + ADR-009 planned. Frontend mocks will consume API when ready.”

**Remediation:** DB or YAML config with effective dates; one backend module; API for frontend checker.

---

### T-M02 — Misleading “AI” naming for rule-based logic

| | |
|--|--|
| **Evidence** | `AIAccountantService`, `getAISummary()`, `DashboardService.getInsights()` — all rules |
| **Code** | Multiple services; UI labels “AI Accountant” |

**Why they care:** **Product/architecture honesty** — regulators and users may assume ML.

**Severity:** **High** (trust)

**How to answer:** “Documented as rule-based in AI audit and UI should say ‘automated insights’. Rebrand optional.”

**Remediation:** Glossary in docs; UI strings “Automated” vs “AI”; when LLM ships, separate badge.

---

### T-M03 — No dedicated API docs for several modules

| | |
|--|--|
| **Evidence** | Transactions/Imports/Reports rely on module md + OpenAPI only |
| **Docs** | `docs/api/` partial coverage |

**Why they care:** Onboarding and **contract governance**.

**Severity:** **Medium**

**How to answer:** “OpenAPI is source of truth; module docs in `docs/modules/`. Gap list in ADR coverage.”

**Remediation:** Export OpenAPI per tag; link from `docs/index.md`.

---

### T-M04 — Frontend mock hybrid (Business Guide)

| | |
|--|--|
| **Evidence** | Articles from API; FOP/KVED/checker from static TS |
| **Code** | `business-guide.service.ts` vs `knowledge.service.ts` |

**Why they care:** **Split brain** — tax numbers may ≠ backend.

**Severity:** **High**

**How to answer:** “Documented in [data-sources.md](data-sources.md). Checker is client-only MVP; articles are DB-backed.”

**Remediation:** ADR-014; migrate static data to API or generate from same `TaxConfiguration`.

---

### T-M05 — Settings not persisted server-side

| | |
|--|--|
| **Evidence** | Preferences in `localStorage` only |
| **Code** | `PreferencesContext.tsx` |

**Why they care:** Multi-device users lose settings; **no server source of truth**.

**Severity:** **Medium**

**How to answer:** “MVP local-first preferences. `X-App-*` headers sync to backend thread-local for formatting only.”

**Remediation:** `user_preferences` table + `PUT /api/users/me/preferences`.

---

### T-M06 — Test pyramid imbalance

| | |
|--|--|
| **Evidence** | Backend 95 unit tests; no frontend tests; limited backend integration in repo CI |
| **Code** | `pom.xml`, `flowiq-frontend/package.json` |

**Why they care:** **Regression risk** on cross-stack flows.

**Severity:** **High**

**How to answer:** “`flowiq-automation` covers contract + regression in separate repo CI. Backend unit tests for engines. E2E on PR in automation.”

**Remediation:** Link CI badges; add smoke in backend repo or document single pipeline truth.

---

### T-M07 — ThreadLocal `AppPreferences` without async audit

| | |
|--|--|
| **Evidence** | `AppPreferencesFilter` sets ThreadLocal; cleared in `finally` |
| **Code** | `config/AppPreferencesFilter.java`, `AppPreferences.java` |

**Why they care:** **`@Async` or reactive** would leak or lose locale.

**Severity:** **Low** (sync servlet MVP)

**How to answer:** “Servlet stack only; no async handlers yet. Pass locale explicitly if we add async.”

**Remediation:** Document constraint; use `LocaleContextHolder` or explicit param in async jobs.

---

### T-M08 — Large service classes

| | |
|--|--|
| **Evidence** | `AIAccountantService`, `TaskService`, `ForecastService` — 400+ lines |
| **Code** | `service/AIAccountantService.java`, etc. |

**Why they care:** **God class** tendency — harder test and review.

**Severity:** **Medium**

**How to answer:** “MVP cohesion per use case. Extract when third similar method appears.”

**Remediation:** Extract `ForecastProjection`, `TaxAdvisorFacade`, `TaskQueryService`.

---

### T-M09 — EMAIL/TELEGRAM notification channels unused

| | |
|--|--|
| **Evidence** | Enum values exist; generator writes IN_APP only |
| **Code** | `notifications/entity/NotificationChannel.java` |

**Why they care:** **Dead domain model** — misleading API consumers.

**Severity:** **Low**

**How to answer:** “Channel enum prepares multi-channel. Only in-app in MVP.”

**Remediation:** Hide from OpenAPI until implemented.

---

### T-M10 — No Flyway in backend-only CI

| | |
|--|--|
| **Evidence** | `backend-ci.yml` — unit tests, no Testcontainers migrate |
| **Code** | `.github/workflows/backend-ci.yml` |

**Why they care:** **Schema drift** undetected until deploy.

**Severity:** **Medium**

**How to answer:** “Automation PR Validation runs contract tests against real Postgres + Flyway via backend JAR. Backend repo CI is fast unit gate.”

**Remediation:** Add Testcontainers Flyway smoke job to backend CI or document automation as schema gate.

---

# 6. Suspicious design decisions

### T-D01 — Why seed fake transactions instead of empty state UX?

| | |
|--|--|
| **Decision** | ADR-002 auto-seed on empty account |
| **Code** | `TransactionSeedService` |

**Why they care:** *“You chose deception over education.”*

**Severity:** **Critical** (ethics/trust)

**How to answer:** “Tradeoff for demo WOW factor in investor/QA demos. We’re moving to labeled demo mode + empty state with guided import.”

**Remediation:** See T-A01; product design for honest empty state.

---

### T-D02 — Why two chat systems?

| | |
|--|--|
| **Evidence** | `ChatController` + `/api/ai-accountant/chat` |
| **Code** | `ChatService` vs `AIAccountantService.chat()` |

**Why they care:** **Duplicate UX and persistence** — `chat_*` tables vs accountant inline responses.

**Severity:** **Medium**

**How to answer:** “Standalone chat = history in DB; AI Accountant chat = module-specific quick Q&A. Merge possible under unified conversation service.”

**Remediation:** Single `ConversationService` with `channel` enum; deprecate duplicate endpoints.

---

### T-D03 — Why generate refresh token without refresh endpoint?

| | |
|--|--|
| **ADR** | [006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md) — endpoint TBD |

**Why they care:** **API design inconsistency** — clients store useless secret.

**Severity:** **High**

**How to answer:** “ADR-006 phased delivery. Token structure ready; endpoint Phase 2. Should not store refresh until then — frontend debt.”

**Remediation:** Stop issuing refresh until endpoint exists, or implement endpoint.

---

### T-D04 — Why `VIEWER` role in schema?

| | |
|--|--|
| **Evidence** | Enum value never assigned or checked |

**Why they care:** **YAGNI** in security model.

**Severity:** **Low**

**How to answer:** “Reserved for accountant read-only portal. Not implemented.”

**Remediation:** Remove from enum until feature exists.

---

### T-D05 — Why is health score calculated in two places?

| | |
|--|--|
| **Evidence** | `DashboardService.getBusinessHealth()` vs `AIAccountantService.getHealth()` — different formulas |
| **Code** | Both services |

**Why they care:** User sees **different “health”** on dashboard vs AI module.

**Severity:** **Medium**

**How to answer:** “Known duplication. Unify under `BusinessHealthCalculator` shared component.”

**Remediation:** Single calculator; both endpoints delegate.

---

### T-D06 — Why store reports in DB not object storage?

| | |
|--|--|
| **Decision** | Simplicity MVP |

**Severity:** **Medium** (see T-A05)

**How to answer:** “Zero external deps for first deploy. Migration path documented.”

**Remediation:** S3-compatible storage ADR.

---

### T-D07 — Why no multi-tenant `company_id`?

| | |
|--|--|
| **Evidence** | Only `user_id` on transactions; `User.company` is string field |

**Why they care:** B2B **accountant manages many clients** roadmap clash.

**Severity:** **Medium** (future)

**How to answer:** “Single-tenant-per-user MVP. `company` is display field. Multi-tenant needs ADR and schema migration.”

**Remediation:** ADR for org model; `organizations` + `membership` when required.

---

### T-D08 — Why `@EnableScheduling` without distributed lock?

| | |
|--|--|
| **Code** | `FlowiqBackendApplication.java` |

**Why they care:** Premature **scale footgun**.

**Severity:** **High** (if K8s replicas > 1)

**How to answer:** “Documented single-replica assumption. ShedLock before HA.”

**Remediation:** See T-A06.

---

### T-D09 — Why open registration without invite-only?

| | |
|--|--|
| **Evidence** | `POST /api/auth/register` public |

**Why they care:** Spam, abuse, unvetted fintech users.

**Severity:** **Medium**

**How to answer:** “Open beta MVP. Invite codes or admin approval for production launch.”

**Remediation:** Feature flag `registration-enabled`; waitlist API.

---

### T-D10 — Why rule-based chat labeled “AI Accountant”?

| | |
|--|--|
| **Evidence** | Keyword templates in `AIAccountantService.chat()`, `ChatService.generateReply()` |

**Why they care:** **Regulatory marketing** risk in UA fintech context.

**Severity:** **High**

**How to answer:** “Assistant = automated rules, not licensed advice. Legal disclaimers in UI (draft in `docs/legal/`). LLM later with human-in-loop.”

**Remediation:** Disclaimer component; legal review; rename to “Financial Assistant” if required.

---

# Review session cheat sheet

### If architect attacks broadly

1. **Acknowledge MVP scope** — rule-based intelligence, single-tenant, monolith.  
2. **Point to docs** — [ARCHITECTURE_REVIEW_READINESS.md](ARCHITECTURE_REVIEW_READINESS.md) scores 71 readiness, 86 doc health.  
3. **Separate demo vs prod** — seed, demo user, secrets = **pre-prod gates** (TD-C01–C06).  
4. **Show CI truth** — backend unit + `flowiq-automation` contract/regression.  
5. **Show remediation roadmap** — [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) 3-month plan.

### Top 10 traps to rehearse

| # | Trap | One-line answer |
|---|------|-----------------|
| 1 | Fake transaction seed | ADR-002 demo; prod flag + `source` column planned |
| 2 | No audit log | TD-C02; ADR-013 before go-live |
| 3 | JWT in localStorage | MVP; HttpOnly + refresh in roadmap |
| 4 | No refresh endpoint | ADR-006 Phase 2 |
| 5 | “AI” without LLM | Rule-based; documented |
| 6 | Dual forecast math | Consolidate to `ForecastEngine` |
| 7 | Tax constants duplicated | ADR-009 `TaxConfigurationService` |
| 8 | Schedulers multi-instance | Single replica until ShedLock |
| 9 | BYTEA reports | Object storage migration planned |
| 10 | RBAC unused | user_id scoping only; roles future |

---

## Related documents

| Document | Link |
|----------|------|
| Interview Q&A (56) | [ARCHITECT_INTERVIEW_GUIDE.md](ARCHITECT_INTERVIEW_GUIDE.md) |
| Request flows | [REQUEST_FLOW_MAP.md](REQUEST_FLOW_MAP.md) |
| Debt register | [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) |
| ADR defense | [ADR_DEFENSE_GUIDE.md](ADR_DEFENSE_GUIDE.md) |
| Component catalog | [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) |

**Total traps documented:** 48  
**Last verified:** 2026-06-23
