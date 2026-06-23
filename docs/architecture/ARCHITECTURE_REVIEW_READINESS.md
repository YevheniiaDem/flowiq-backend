# Architecture Review Readiness Report

**Audit date:** 2026-06-23  
**Scope:** `flowiq-backend`, `flowiq-frontend` (code + `flowiq-backend/docs/`)  
**Method:** Full codebase trace; documentation aligned to as-built state only  
**Audience:** Senior Architect review  
**Latest AI audit:** [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md)

---

## Executive Summary

FlowIQ is a **functional MVP** — Spring Boot backend with PostgreSQL, Next.js frontend, JWT auth, and a **rule-based intelligence layer**. The platform is suitable for architectural review with clear caveats: demo data seeding, hybrid frontend mocks, no CI/CD, no external LLM/bank APIs, and several cross-cutting gaps (audit log, settings backend).

This audit **created C4 diagrams, data-source truth table, and AI layer as-built docs**, and **corrected stale documentation** that contradicted the codebase.

---

## What Was Found (As-Built)

### Platform

| Area | Finding |
|------|---------|
| **Backend** | Spring Boot 3.5, Java 17, 15+ REST controllers, Flyway V1–V5, JWT auth implemented |
| **Frontend** | Next.js 16, React 19 — most modules call real API; Business Guide profile/taxes/KVED and tax profile card use static mock data |
| **Database** | PostgreSQL 15 — `users`, `transactions`, `tasks`, `notifications`, `knowledge_articles`, `import_jobs`, `report_jobs`, `chat_*` |
| **CI/CD** | Not implemented — no GitHub Actions / GitLab CI |
| **Docker** | Dockerfiles exist in **both** repos; `compose.yaml` is PostgreSQL-only |
| **Tests** | Backend: 9 test classes (unit + PDF/Excel renderers), JaCoCo; Frontend: none |
| **External APIs** | None — no bank HTTP clients, no LLM SDKs |

### Intelligence Layer

| Expected (design names) | Actual in code |
|------------------------|----------------|
| `AiQualityFactory` | **Absent** — Spring DI + `List<Provider>` injection |
| `*Orchestrator` classes | **Absent** — domain services (`AIAccountantService`, `ForecastService`, etc.) |
| `*Agent.java` classes | **Absent** — 11 intelligence units as `@Component` rule engines |
| LLM providers | **Interfaces only** — `AIInsightProvider`, `ForecastProvider`, etc.; `AnalyticsInsightProvider` injected but never called; `TransactionInsightService` has no callers |

### Data Integrity Risks for Review

1. **`TransactionSeedService`** — auto-seeds 6 months of demo transactions into PostgreSQL on first access to dashboard/analytics/forecasts/reports/AI Accountant/chat/tasks. Metrics appear populated without real user data.
2. **Hardcoded FOP/tax constants** — duplicated across `AnalyticsService`, `ForecastService`, `NotificationRuleEngine`, `TaskRuleEngine` (not externalized).
3. **Frontend mock split** — Business Guide articles = API/DB; groups/taxes/KVED/checker = local static files.

### Documentation Gaps (Before Audit)

| Issue | Severity |
|-------|----------|
| No C4 diagrams | High |
| No single data-sources document | High |
| `README.md` stated JWT/PostgreSQL/API as "planned" | High |
| `docker.md` stated "No Dockerfile" | Medium |
| `test-strategy.md` stated "zero tests" | Medium |
| `COVERAGE-REPORT.md` listed missing Dockerfile, zero tests | Medium |
| AI Factory docs referenced non-existent classes | High (if written aspirational) |

---

## What Was Fixed / Created

### New Documents

| File | Purpose |
|------|---------|
| [c4/c4-context.md](c4/c4-context.md) | C4 Level 1 — User, FlowIQ, PostgreSQL, CSV, planned Bank/AI |
| [c4/c4-container.md](c4/c4-container.md) | C4 Level 2 — Frontend, Backend, DB, Scheduler/AI/Reporting/Import layers |
| [data-sources.md](data-sources.md) | Single source of truth for all module data origins |
| [ai-quality-factory.md](ai-quality-factory.md) | Intelligence layer as-built; maps conceptual 3-level model to real classes |
| [ai-agents-architecture.md](ai-agents-architecture.md) | All 11 intelligence units — inputs, outputs, use cases |
| **This report** | Review readiness summary + health score |

### Corrected Documents

| File | Change |
|------|--------|
| `README.md` | Reflects implemented JWT, PostgreSQL, Flyway, all MVP endpoints, unit tests, Dockerfiles |
| `docs/deployment/docker.md` | Documents existing Dockerfiles (backend + frontend); removed "No Dockerfile" |
| `docs/qa/test-strategy.md` | Documents 9 backend test classes; clarifies no frontend/integration/E2E tests |
| `docs/COVERAGE-REPORT.md` | Dockerfiles ✅, unit tests ✅, architecture docs count updated |
| `docs/index.md` | Links to all new architecture documents |

---

## Remaining Gaps (Post-Audit)

### Architecture & Documentation

| Gap | Priority | Notes |
|-----|----------|-------|
| C4 Component diagram (Level 3) | Medium | Package-level diagram for backend modules not created |
| ADR-009: FOP/tax constants externalization | Medium | See [ADR Coverage Report](adr/ADR_COVERAGE_REPORT.md) |
| `system-overview.md` sync | Low | May still reference pre-audit assumptions — verify on next pass |
| Dedicated API docs for Transactions/Imports/Reports/Analytics/Chat | Medium | Module docs only |
| Audit Log | High | 0% — no entity, table, API, or UI |
| User Settings backend | High | Frontend-only; no persistence API |
| CI/CD as-built doc vs implementation | Medium | `ci-cd.md` correctly states "not configured" |

### Code / Platform (Out of Scope for This Audit)

| Gap | Priority |
|-----|----------|
| CI/CD pipelines | High |
| Integration tests (Testcontainers) | High |
| Frontend tests (Vitest/Playwright) | Medium |
| LLM provider implementation | Planned |
| Bank API integrations | Planned (feature flag off) |
| Email/Telegram notification delivery | Medium |
| Role-based authorization enforcement | Medium |
| Full-stack docker-compose | Low |

---

## Recommendations for Senior Architect Review

### Session 1 — System Context (30 min)

1. Walk through [C4 Context](c4/c4-context.md) and [C4 Containers](c4/c4-container.md).
2. Confirm boundaries: what is in MVP vs planned (banks, LLM).
3. Discuss **TransactionSeedService** — is DB-backed demo acceptable for production onboarding?

### Session 2 — Data & Trust (45 min)

1. Review [Data Sources](data-sources.md) module matrix.
2. Decide policy: **real data vs demo data** labeling in UI/API responses.
3. Review hardcoded FOP/tax constants — centralize vs config service.

### Session 3 — Intelligence Layer (45 min)

1. Review [AI Quality Factory](ai-quality-factory.md) and [AI Agents Architecture](ai-agents-architecture.md).
2. Confirm rule-based approach is acceptable until LLM providers ship (ADR-001).
3. Discuss dual health scoring (`DashboardService` vs `AIAccountantService`).

### Session 4 — Gaps & Roadmap (30 min)

1. Prioritize: Audit Log, Settings backend, CI/CD — see [ADR Index](adr/README.md).
2. Bank integrations phased rollout per [Bank Integrations Roadmap](../roadmap/BANK_INTEGRATIONS_ROADMAP.md).
3. Security: JWT in prod, role enforcement, data isolation between users.

### Questions to Prepare Answers For

| Question | Current Answer |
|----------|----------------|
| Is AI real or mock? | **Rule-based** — deterministic thresholds; no LLM calls |
| Where does transaction data come from? | User CRUD, CSV import, or **auto-seed** if empty |
| Multi-tenancy? | Single-user rows (`user_id` FK); no `companies` table |
| How are FOP limits updated? | **Hardcoded** in Java — manual code change required |
| Production deployment? | Frontend likely Vercel; backend TBD; no automated pipeline |

---

## Architecture Documentation Health Score

Scoring model: 0–100 per dimension, weighted for architect review readiness.

| Dimension | Weight | Score | Rationale |
|-----------|--------|-------|-----------|
| **C4 / structural diagrams** | 20% | **85** | Context + Container created; Component level missing |
| **Data source transparency** | 20% | **90** | `data-sources.md` covers all 11 modules + seed service |
| **AI layer documentation** | 15% | **92** | Code-verified audit 2026-06-23; component registry + discrepancy fixes |
| **Accuracy (docs ↔ code)** | 20% | **88** | AI docs corrected; module docs may still lag |
| **ADR / decisions** | 10% | **78** | ADR-001–008 documented; tax constants, RBAC, CI/CD gaps remain |
| **Operational readiness docs** | 10% | **70** | Docker accurate; CI/CD correctly marked absent |
| **Cross-cutting features** | 5% | **40** | Audit log, settings, RBAC gaps documented but not solved |

### Calculation

```
(85×0.20) + (90×0.20) + (92×0.15) + (88×0.20) + (78×0.10) + (70×0.10) + (40×0.05)
= 17.0 + 18.0 + 13.8 + 17.6 + 7.8 + 7.0 + 2.0
= 83.2 → 83/100
```

## Final Score

# Architecture Documentation Health Score: **83 / 100**

| Grade | Range | Status |
|-------|-------|--------|
| A | 90–100 | Production-grade architecture docs |
| B | 75–89 | **← Current: Ready for review with known gaps** |
| C | 60–74 | Partial — major gaps block confident review |
| D | <60 | Not ready |

### Score Trajectory

| Date | Score | Delta | Driver |
|------|-------|-------|--------|
| Pre-audit (2026-06-11) | **62** | — | No C4, stale README/docker/tests, no data-sources |
| Post-audit (2026-06-17) | **79** | **+17** | C4, data-sources, AI as-built docs, contradiction fixes |
| Post-ADR (2026-06-17) | **81** | **+2** | ADR-002–008, ADR Coverage Report |
| Post-AI audit (2026-06-23) | **83** | **+2** | [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md) |

### To Reach 90+ (Grade A)

1. Add C4 Component diagram for backend packages.
2. Write ADR-009 (FOP/tax constants) and ADR-017 (authorization model).
3. Sync `system-overview.md` and module docs with `data-sources.md`.
4. Document Audit Log and Settings as explicit "not implemented" architecture decisions.
5. Add CI/CD as-built workflow doc when pipeline is implemented.

---

## Document Index (Architecture Hub)

| Document | Link |
|----------|------|
| C4 Context | [c4/c4-context.md](c4/c4-context.md) |
| C4 Containers | [c4/c4-container.md](c4/c4-container.md) |
| Data Sources | [data-sources.md](data-sources.md) |
| AI Quality Factory | [ai-quality-factory.md](ai-quality-factory.md) |
| AI Agents | [ai-agents-architecture.md](ai-agents-architecture.md) |
| AI Documentation Audit | [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md) |
| ADR Index | [adr/README.md](adr/README.md) |
| ADR Coverage | [adr/ADR_COVERAGE_REPORT.md](adr/ADR_COVERAGE_REPORT.md) |
| Full docs index | [../index.md](../index.md) |

---

**Prepared by:** Architecture documentation audit (2026-06-17)  
**Next review:** After ADR-009/017 and C4 Component diagram, or before production go-live
