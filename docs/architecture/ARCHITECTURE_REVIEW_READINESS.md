# Architecture Review Readiness Report

**Final audit date:** 2026-06-23  
**Scope:** `flowiq-backend`, `flowiq-frontend` (code) + `flowiq-backend/docs/`  
**Method:** Full documentation cross-check against code; workflows, migrations, security, AI call-graph trace  
**Audience:** Senior Architect review  
**Prior audits:** 2026-06-17 (C4/ADR), 2026-06-23 (AI, debt, CI/CD, component catalog, interview guide)

---

## Executive Summary

FlowIQ is a **demonstrable MVP** suitable for **architectural review sessions** with explicit caveats. Documentation is **substantially aligned** with as-built code (C4, ADRs, AI audit, CI as-built, component catalog, 56-question interview guide). **Production sign-off** remains blocked by data-integrity, security, and operational gaps in [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md).

| Score | Value | Meaning |
|-------|-------|---------|
| **Architecture Documentation Health** | **86 / 100** | Documentation package is strong; minor staleness and missing L3 C4 |
| **Architecture Review Readiness** | **71 / 100** | MVP is explainable and demo-ready; production trust & ops maturity insufficient |

**Verdict:** Proceed with architect review **for MVP architecture, decisions, and roadmap** — not for production go-live approval without a remediation plan.

---

## Final Audit — Documentation Areas

### Summary matrix

| Area | Docs reviewed | Alignment with code | Gaps / staleness |
|------|---------------|---------------------|------------------|
| **Architecture hub** | `system-overview.md`, C4 L1–L2, `SYSTEM_COMPONENT_CATALOG.md`, cheat sheet, interview guide | **High** | C4 Component (L3) missing; `system-overview.md` production/Docker line **fixed** in this audit |
| **ADR** | ADR-001–008, `ADR_COVERAGE_REPORT.md`, `ADR_DEFENSE_GUIDE.md` | **High** | ~72% decision coverage; ADR-009+ not written |
| **AI** | `ai-quality-factory.md`, `ai-agents-architecture.md`, `docs/ai/*`, `AI_DOCUMENTATION_AUDIT_REPORT.md` | **Very high** | Rule-based only; dead/unused beans documented |
| **CI/CD** | `ci-cd.md`, `ci-cd-as-built.md`, `CI_CD_EVOLUTION_PLAN.md`, `CI_READINESS_REPORT.md` | **High** | CI ✅ both repos; CD ❌; no Flyway/Testcontainers/Docker in CI |
| **Deployment** | `docker.md`, `production-deployment.md`, `environments.md`, `local-setup.md` | **High** | Checklists accurate; staging TBD; no automated deploy |
| **Security** | `authentication.md`, `jwt-flow.md`, `authorization.md`, `data-protection.md` | **High** | Gaps documented (refresh, localStorage, no `@PreAuthorize`) |
| **Database** | `database-architecture.md`, `migrations.md`, `schema-overview.md`, `relationships.md` | **High** | FK gaps on notifications/import/report jobs in catalog |

### Architecture documents (verified)

| Document | Status |
|----------|--------|
| [c4/c4-context.md](c4/c4-context.md) | ✅ User, FlowIQ, PostgreSQL, CSV; Bank/LLM external (planned) |
| [c4/c4-container.md](c4/c4-container.md) | ✅ Frontend, Backend, DB, schedulers |
| [data-sources.md](data-sources.md) | ✅ Per-module real vs seed vs mock |
| [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) | ✅ 13 controllers, 9 tables, production flags |
| [ARCHITECT_INTERVIEW_GUIDE.md](ARCHITECT_INTERVIEW_GUIDE.md) | ✅ 56 Q&A code-verified |
| [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) | ✅ 48 items (6 Critical) |

### ADR (verified)

| ADR | Code anchor | Match |
|-----|-------------|-------|
| 001 Pluggable AI | `AIInsightProvider`, `RuleBasedForecastProvider` | ✅ |
| 002 Transaction seed | `TransactionSeedService.seedIfEmpty()` | ✅ (risk: all environments) |
| 003 AI quality factory | Distributed services, no orchestrator | ✅ |
| 004 PostgreSQL | `compose.yaml`, `application.properties` | ✅ |
| 005 Flyway | V1–V5, `ddl-auto=validate` | ✅ |
| 006 JWT | `JwtService`, `JwtAuthenticationFilter` | ✅ refresh endpoint gap |
| 007 Layered | Controller → Service → Repository | ✅ |
| 008 Frontend | `app/`, `src/features/`, `api.ts` | ✅ |

**Undocumented (ADR-009+ candidates):** FOP/tax constants strategy, audit log absence, frontend mock hybrid, monolith deployment, refresh-token lifecycle, scheduler scale-out.

### AI documents (verified)

| Claim in docs | Code truth |
|---------------|------------|
| Production AI = rule-based | ✅ No LLM SDK in `pom.xml` |
| `TransactionInsightService` unused | ✅ Zero callers |
| `AnalyticsInsightProvider` unused | ✅ Injected, never invoked |
| `AIAccountantService.getForecasts()` ≠ `ForecastEngine` | ✅ Inline `buildForecast()` |
| `DashboardService.getInsights()` inline rules | ✅ |
| Frontend has no AI engines | ✅ HTTP clients only |

### CI/CD documents (verified)

| Claim | Code truth |
|-------|------------|
| Backend CI on push/PR `main` | ✅ `.github/workflows/backend-ci.yml` |
| `mvnw clean verify`, Java 17 | ✅ |
| `SPRING_DOCKER_COMPOSE_ENABLED=false` in CI | ✅ |
| JaCoCo artifact upload | ✅ |
| Frontend lint + build | ✅ `flowiq-frontend/.github/workflows/frontend-ci.yml` |
| CD / deploy on merge | ❌ |
| ~95 unit tests in CI | ✅ Surefire `*Test.java` only; `FlowiqBackendApplicationTests` excluded |

### Deployment documents (verified)

| Claim | Code truth |
|-------|------------|
| `compose.yaml` = PostgreSQL only | ✅ |
| Backend `Dockerfile` multi-stage JRE 17 | ✅ Healthcheck `GET /api/health` |
| Production checklist (secrets, CORS, show-sql) | ✅ Matches risks in `application.properties` |

### Security documents (verified)

| Claim | Code truth |
|-------|------------|
| Stateless JWT, BCrypt | ✅ |
| Public: health, register, login, Swagger | ✅ |
| Filter accepts access tokens only | ✅ `isAccessToken()` |
| Refresh issued, no `/auth/refresh` | ✅ |
| Roles ADMIN/USER/VIEWER | ✅ `User.Role` — not enforced per endpoint |
| Token in `localStorage` | ✅ |

### Database documents (verified)

| Claim | Code truth |
|-------|------------|
| 9 tables, Flyway V1–V5 | ✅ |
| `validate` + Flyway enabled | ✅ |
| `report_jobs.file_content` BYTEA | ✅ |
| Partial unique dedup indexes | ✅ V3, V4 |
| V5 seeds knowledge articles | ✅ |

### Remaining documentation staleness

| Item | Severity |
|------|----------|
| `test-strategy.md` — test count ~88+ vs **95** in CI | Low |
| `COVERAGE-REPORT.md` — file list predates 2026-06-23 artifacts | Low |
| Dedicated API md for Transactions/Imports/Reports/Analytics/Chat | Medium |
| C4 Component diagram | Medium |
| Formal ADR for audit-log **absence** | Medium |

---

## Platform As-Built (Code)

| Area | Finding |
|------|---------|
| **Backend** | Spring Boot 3.5.14, Java 17, **13** REST controllers, Flyway V1–V5, JWT |
| **Frontend** | Next.js 16, React 19 — API-backed core; Business Guide / tax / integrations partially mocked |
| **Database** | PostgreSQL 15 — 9 business tables |
| **CI** | GitHub Actions both repos; **CD not implemented** |
| **Tests** | Backend 9 `*Test.java` classes, **95** methods; Frontend **none** in CI |
| **External APIs** | None — no bank clients, no LLM SDKs |
| **Monitoring** | `/api/health` only — no Actuator |

---

# Architecture Documentation Health Score

**Measures:** completeness and **accuracy** of the documentation set.

| Dimension | Weight | Score | Rationale |
|-----------|--------|-------|-----------|
| C4 / structural diagrams | 14% | **84** | Context + Container ✅; Component (L3) ❌ |
| Data & component transparency | 16% | **93** | `data-sources.md` + `SYSTEM_COMPONENT_CATALOG.md` |
| AI layer documentation | 12% | **95** | AI audit + aligned `docs/ai/*` |
| Accuracy (docs ↔ code) | 18% | **87** | Minor count/staleness; system-overview fixed |
| ADR / decision traceability | 10% | **82** | 8 ADRs + defense guide; ~28% decisions undocumented |
| CI/CD & deployment docs | 12% | **83** | As-built CI accurate; CD/monitoring mostly planned |
| Review artifacts | 10% | **94** | Interview guide, cheat sheet, debt register, catalog |
| Cross-cutting feature docs | 8% | **52** | Audit/settings gaps documented, not ADR-closed |

```
(84×0.14) + (93×0.16) + (95×0.12) + (87×0.18) + (82×0.10) + (83×0.12) + (94×0.10) + (52×0.08)
= 85.4 → 86/100
```

## Architecture Documentation Health Score: **86 / 100** (Grade B+)

| Grade | Range |
|-------|-------|
| **A** | 90–100 |
| **B** | 75–89 ← **current** |
| **C** | 60–74 |
| **D** | <60 |

---

# Architecture Review Readiness Score

**Measures:** holistic readiness for architect review — MVP + defensibility + production risk (not docs alone).

| Dimension | Weight | Score | Rationale |
|-----------|--------|-------|-----------|
| MVP functional completeness | 12% | **88** | 11 domain modules with UI + API |
| Documentation package | 18% | **86** | Aligned with code; review artifacts complete |
| Security posture | 14% | **58** | Dev secrets, localStorage JWT, incomplete refresh |
| Data integrity & trust | 14% | **52** | Auto-seed, no `transactions.source` |
| Test evidence | 9% | **64** | 95 unit tests; no integration/E2E/FE tests |
| CI/CD & release | 9% | **60** | CI only; no staging/CD/Docker in pipeline |
| Observability & ops | 5% | **38** | No Actuator, no structured ops runbook as-built |
| ADR / governance | 9% | **78** | Core stack decided; production gaps not ADR-closed |
| Explainability (Q&A readiness) | 10% | **92** | Interview guide, cheat sheet, ADR defense |

```
(88×0.12) + (86×0.18) + (58×0.14) + (52×0.14) + (64×0.09) + (60×0.09) + (38×0.05) + (78×0.09) + (92×0.10)
= 70.7 → 71/100
```

## Architecture Review Readiness Score: **71 / 100** (Grade C+ / MVP-ready)

Interpretation: **Ready to present and defend architecture**; **not ready** for production launch sign-off.

---

## Score Trajectory

| Date | Doc Health | Review Readiness | Driver |
|------|------------|------------------|--------|
| 2026-06-11 (pre-audit) | **62** | **~55** | Stale README, no C4, no data-sources |
| 2026-06-17 | **79** | **~65** | C4, data-sources, AI as-built, ADR-002–008 |
| 2026-06-23 (AI audit) | **83** | **~68** | AI doc corrections |
| **2026-06-23 (final)** | **86** | **71** | Catalog, interview guide, debt register, CI verification |

---

## Remaining Risks (Prioritized)

### Critical (block production trust)

| ID | Risk | Evidence |
|----|------|----------|
| R1 | **Synthetic transaction seed** indistinguishable from real data | `TransactionSeedService` — TD-C01, TD-C04 |
| R2 | **No audit log** for financial / tax / AI actions | No table V1–V5 — TD-C02 |
| R3 | **Secrets in default config** (`jwt.secret`, DB password, `show-sql=true`) | `application.properties` — TD-C03 |
| R4 | **Hardcoded FOP/tax constants** across 4+ services | TD-C05 — inconsistent law updates |
| R5 | **Incomplete JWT lifecycle** (refresh unused, no revocation) | TD-C06 |

### High (review will challenge)

| ID | Risk | Evidence |
|----|------|----------|
| R6 | Demo user `demo@flowiq.ai` / `demo123` seeded on startup | `DemoUserSeedService` |
| R7 | JWT in `localStorage` (XSS surface) | `api.ts`, `auth.service.ts` |
| R8 | RBAC roles exist but **not enforced** | `SecurityConfig`, no `@PreAuthorize` |
| R9 | **Dual forecast paths** may diverge | `ForecastEngine` vs `AIAccountantService.buildForecast()` |
| R10 | **Dead AI hooks** look like unfinished features | `TransactionInsightService`, unused providers |
| R11 | Schedulers run on **every instance** (no leader election) | `DailyTaskScheduler`, `NotificationScheduler` |
| R12 | Reports stored as **BYTEA** in PostgreSQL | `report_jobs` — backup/scale risk |
| R13 | **No integration/E2E tests**; Flyway not validated against PG in CI | workflows, `pom.xml` |
| R14 | **Frontend mock hybrid** — user may think all Business Guide data is authoritative | `business-guide.service.ts` |

### Medium

| ID | Risk |
|----|------|
| R15 | Missing FK on `notifications`, `import_jobs`, `report_jobs` → `users` |
| R16 | CORS allowlist hardcoded — new prod origin needs code change |
| R17 | `JwtAuthenticationFilter` swallows parse errors silently |
| R18 | Settings / preferences not persisted server-side |
| R19 | Bank integrations flag off; UI partially mock |
| R20 | EMAIL/TELEGRAM notification channels enum-only |

---

## Possible Architect Questions

Full bank: **[ARCHITECT_INTERVIEW_GUIDE.md](ARCHITECT_INTERVIEW_GUIDE.md)** (56 questions).  
Defense depth: **[ADR_DEFENSE_GUIDE.md](ADR_DEFENSE_GUIDE.md)**.

### Top 20 likely questions

| # | Question | Short answer |
|---|----------|--------------|
| 1 | Is AI real or LLM? | **Rule-based only** — no LLM SDK |
| 2 | Where does dashboard data come from? | User data, CSV, or **auto-seed** if empty |
| 3 | Can users tell demo from real transactions? | **No** — no `source` column |
| 4 | Multi-tenancy model? | Row-level `user_id`; single DB |
| 5 | How are FOP limits updated? | **Hardcoded Java** — manual deploy |
| 6 | Refresh token flow? | Issued; **`/auth/refresh` missing** |
| 7 | Token storage? | **localStorage** |
| 8 | Role-based access? | Roles in JWT; **not enforced** on endpoints |
| 9 | CI/CD today? | **CI yes** (both repos); **CD no** |
| 10 | Test strategy? | 95 backend unit tests; no FE/E2E |
| 11 | Schema migrations? | Flyway V1–V5; validate mode |
| 12 | Why `TransactionInsightService`? | Future hook; **zero callers** |
| 13 | Forecast Center vs AI Accountant forecasts? | **Different code paths** |
| 14 | Audit trail? | **None** |
| 15 | Production deployment? | Docker manual; FE likely Vercel |
| 16 | Horizontal scale? | API stateless OK; **schedulers duplicate** |
| 17 | Monitoring? | `/api/health` only |
| 18 | Why pluggable providers with no LLM beans? | ADR-001 forward compatibility |
| 19 | Business Guide — all from API? | **Mixed** — articles API; FOP/KVED mock |
| 20 | Biggest production blocker? | **Data trust** (seed + no audit + tax constants) |

---

## Project Weaknesses (Weak Spots)

| Category | Weakness |
|----------|----------|
| **Data trust** | Auto-seed writes realistic financial history without labeling |
| **Compliance** | No audit log; tax advice from hardcoded rules without versioning |
| **Security** | Dev-grade secrets, localStorage tokens, incomplete session lifecycle |
| **Architecture consistency** | Duplicate forecast/tax logic; dead beans in DI graph |
| **Testing** | Thin coverage surface; no contract/integration tests |
| **Operations** | No Actuator, no staging pipeline, no automated smoke |
| **Frontend** | Client-side auth guard only; partial mocks presented as product |
| **Documentation** | Strong hub; module/API leaf docs and C4 L3 still thin |
| **Governance** | ~28% architectural decisions lack formal ADR |

---

## Recommendations to Reach 90+ / 100

### Documentation Health → 90+ (Grade A)

| Priority | Action | Expected impact |
|----------|--------|-----------------|
| 1 | Add **C4 Component diagram** (`c4/c4-component.md`) | +3–4 pts |
| 2 | Write **ADR-009** (tax/FOP config) and **ADR-013** (audit log decision) | +2–3 pts |
| 3 | Sync `test-strategy.md`, `COVERAGE-REPORT.md`, module docs with catalog | +1–2 pts |
| 4 | Add dedicated API docs or OpenAPI export links per module | +1–2 pts |
| 5 | ADR for **intentional gaps**: settings backend, mock hybrid strategy | +2 pts |

### Architecture Review Readiness → 90+ (production-grade)

| Priority | Action | Closes risk |
|----------|--------|-------------|
| 1 | `flowiq.features.demo-seed-enabled=false` in prod + UI banner | R1 |
| 2 | `V6`: `transactions.source` + API exposure | R1, R4 |
| 3 | Externalize secrets; `application-prod.properties`; fail on default JWT | R3 |
| 4 | `POST /api/auth/refresh` + axios interceptor | R5 |
| 5 | `AuditLogService` on sensitive operations | R2 |
| 6 | `TaxConfigurationService` single source of truth | R4 |
| 7 | Testcontainers + Flyway in CI; staging deploy workflow | R13 |
| 8 | Spring Actuator + secured health/metrics | R17 |
| 9 | Unify forecast path or document contract test between engines | R9 |
| 10 | Remove or gate `DemoUserSeedService` in prod | R6 |

**Realistic target:** Documentation **90+** achievable in **1–2 doc sprints**; Review Readiness **90+** requires **code + ops** work (estimate **2–3 months** per [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) roadmap).

---

## What May Still Raise Questions When Reviewing Code

Items **not obvious from docs alone** — architect may find in IDE / PR walkthrough:

| # | Code finding | Why it triggers questions |
|---|--------------|---------------------------|
| 1 | `JwtAuthenticationFilter` — `catch (Exception ignored)` | Silent auth failures; hard to debug security incidents |
| 2 | `DemoUserSeedService` logs **password** at INFO on create | Security hygiene red flag |
| 3 | `generateRefreshToken()` exists but filter **rejects** refresh tokens | Looks half-implemented |
| 4 | `AuthController.logout()` returns **204 with no server state change** | Misleading "session invalidation" |
| 5 | `TransactionInsightService` Spring bean with **no injection sites** | Dead code in production classpath |
| 6 | `AnalyticsService` constructor takes `List<AnalyticsInsightProvider>` — **never used** | Incomplete refactor |
| 7 | `AIAccountantService.chat()` loops empty `insightProviders` then templates | "AI chat" naming vs template replies |
| 8 | `NotificationRuleEngine` imports task types — **tight coupling** across domains | Boundary smell |
| 9 | `userRepository.findAll()` in schedulers | O(users) memory; no pagination |
| 10 | `import_jobs` / `report_jobs` **no FK** to users | Referential integrity question |
| 11 | `GlobalExceptionHandler` on same `@RestController` pattern | Unusual packaging (minor) |
| 12 | `spring.jpa.show-sql=true` in committed properties | SQL leakage risk if copied to prod |
| 13 | Frontend `authService.isAuthenticated()` — **token presence only**, no expiry check client-side | Stale token UX |
| 14 | `business-guide.service.ts` static tax numbers **≠** backend constants | Cross-stack consistency |
| 15 | Surefire excludes `*Tests.java` — context load test **not in CI** | False confidence in "green" build |
| 16 | `FeatureFlags.bankIntegrationsEnabled` — flag exists, **no runtime branch** in import flow | Dead configuration |
| 17 | Report `BYTEA` in same DB as transactions | Capacity planning question |
| 18 | `KnowledgeService` provider priority logic (skip DB provider if others exist) | Subtle ADR-001 behavior |
| 19 | No `@Transactional` on some read-heavy controller paths | Consistency under load (minor) |
| 20 | Package split: controllers in root vs `tasks/`, `forecasts/`, `knowledge/` | Layer consistency / future modularization |

---

## Recommended Review Sessions

### Session 1 — Context & boundaries (30 min)

1. [C4 Context](c4/c4-context.md) + [C4 Containers](c4/c4-container.md)
2. MVP vs planned: banks, LLM ([data-sources.md](data-sources.md))
3. **TransactionSeedService** policy for production

### Session 2 — Data & trust (45 min)

1. [Data Sources](data-sources.md) module matrix
2. Tax/FOP constants — ADR-009 proposal
3. Audit log requirements (TD-C02)

### Session 3 — Intelligence layer (45 min)

1. [AI Quality Factory](ai-quality-factory.md) + [AI Agents](ai-agents-architecture.md)
2. [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md)
3. Dual forecast paths — consolidate or accept?

### Session 4 — Security, CI/CD, roadmap (30 min)

1. [JWT flow](../security/jwt-flow.md) + refresh gap
2. [CI/CD as-built](../deployment/ci-cd-as-built.md) + [evolution plan](../deployment/CI_CD_EVOLUTION_PLAN.md)
3. [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) — prioritize Month 1–3

---

## Document Index

| Document | Link |
|----------|------|
| C4 Context | [c4/c4-context.md](c4/c4-context.md) |
| C4 Containers | [c4/c4-container.md](c4/c4-container.md) |
| Data Sources | [data-sources.md](data-sources.md) |
| System Component Catalog | [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) |
| Request Flow Map | [REQUEST_FLOW_MAP.md](REQUEST_FLOW_MAP.md) |
| Architect Interview Guide (56 Q) | [ARCHITECT_INTERVIEW_GUIDE.md](ARCHITECT_INTERVIEW_GUIDE.md) |
| Architect Cheat Sheet | [ARCHITECT_REVIEW_CHEAT_SHEET.md](ARCHITECT_REVIEW_CHEAT_SHEET.md) |
| ADR Defense Guide | [ADR_DEFENSE_GUIDE.md](ADR_DEFENSE_GUIDE.md) |
| Technical Debt Register | [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) |
| AI Documentation Audit | [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md) |
| CI/CD As-Built | [../deployment/ci-cd-as-built.md](../deployment/ci-cd-as-built.md) |
| ADR Index | [adr/README.md](adr/README.md) |
| Full docs index | [../index.md](../index.md) |

---

**Prepared by:** Final architecture documentation audit  
**Next review:** After C4 Component diagram + ADR-009/013, or before production go-live
