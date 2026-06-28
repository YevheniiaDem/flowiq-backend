# Automation Architecture

**As-built:** 2026-06-28  
**Repository:** `flowiq-automation`  
**Stack:** Java 17, TestNG, Rest Assured, Playwright (Java), Maven profiles

## Purpose

`flowiq-automation` is a **cross-repository test harness** that validates `flowiq-backend` and `flowiq-frontend` together. It is not a deployable application — it orchestrates API, UI, contract, and E2E tests in CI.

## System Context

```mermaid
flowchart TB
    subgraph Automation["flowiq-automation"]
        API[API Tests Rest Assured]
        UI[UI Tests Playwright]
        E2E[E2E Journey Tests]
        CONTRACT[Contract Tests JSON Schema]
        AGENTS[Optional traceability agents]
    end

    subgraph Targets
        BE[flowiq-backend :8080]
        FE[flowiq-frontend :3000]
        PG[(PostgreSQL :5432)]
    end

    subgraph CI["GitHub Actions"]
        PR[pr-validation.yml]
        NIGHTLY[nightly-regression.yml]
        SMOKE_API[api-smoke.yml]
        SMOKE_UI[ui-smoke.yml]
    end

    PR & NIGHTLY & SMOKE_API & SMOKE_UI --> Automation
    API & CONTRACT --> BE
    E2E & UI --> FE
    BE --> PG
    FE --> BE
```

## Test Layer Structure

```mermaid
flowchart TB
    subgraph Layers
        UNIT_BE[Backend unit tests in flowiq-backend]
        CONTRACT[Contract tests]
        API_SMOKE[API smoke]
        API_REG[API regression]
        UI_SMOKE[UI smoke]
        UI_REG[UI regression]
        E2E[E2E journeys]
    end

    UNIT_BE --> CONTRACT
    CONTRACT --> API_SMOKE --> API_REG
    API_SMOKE --> UI_SMOKE --> UI_REG
    API_REG & UI_REG --> E2E
```

| Layer | Location | Runner |
|-------|----------|--------|
| Backend unit | `flowiq-backend/src/test` | Surefire in backend CI |
| Contract | `flowiq-automation` `-Pcontract` | TestNG + live backend |
| API smoke/regression | `flowiq-automation` `-Papi-smoke` / `-Papi-regression` | Rest Assured |
| UI smoke/regression | `flowiq-automation` `-Pui-smoke` / `-Pui-regression` | Playwright Java |
| E2E | `flowiq-automation` E2E package | Playwright + API setup |

## Maven Profiles (Key)

| Profile | Purpose |
|---------|---------|
| `contract` | OpenAPI JSON Schema validation against running backend |
| `api-smoke` | Fast API health + auth + critical paths |
| `api-regression` | Full API suite |
| `ui-smoke` | Login shell + key pages |
| `ui-regression` | Extended UI coverage |
| `e2e` | End-to-end user journeys |

Environment flag: `-Denv=local|ci|stage|dev`

## PR Validation Architecture

```mermaid
sequenceDiagram
    participant GH as GitHub PR
    participant AUTO as flowiq-automation
    participant BE as flowiq-backend checkout
    participant PG as PostgreSQL service

    par Parallel
        GH->>AUTO: compile job
        GH->>BE: unit-tests job (com.flowiq.unit.**)
    end
    AUTO->>PG: Start PostgreSQL 15
    AUTO->>BE: Build JAR + start on :8080
    AUTO->>AUTO: wait-for-backend /api/health
    AUTO->>AUTO: mvn test -Pcontract
    AUTO->>GH: Pass/fail gate
```

**Workflow:** `.github/workflows/pr-validation.yml`

## Nightly Regression Architecture

Ephemeral Docker stack — build once, parallel test jobs, teardown.

```mermaid
flowchart TB
    TRIGGER[Cron 03:00 UTC / manual] --> BUILD[build-environment]
    BUILD --> SMOKE[smoke]
    BUILD --> API[api-regression]
    BUILD --> UI[ui-regression]
    BUILD --> CONTRACT[contract]
    SMOKE & API & UI & CONTRACT --> TEARDOWN[teardown-environment]
    TEARDOWN --> SUMMARY[regression-summary + Allure]
```

Shared stack reuse via GHCR images or `ci-images.tar` fallback — see `flowiq-automation/docs/automation/CI_INFRASTRUCTURE.md`.

## Feature Traceability

21 features mapped in `docs/qa/TRACEABILITY_MATRIX.md`:

| Feature area | API tests | UI/E2E |
|--------------|-----------|--------|
| Auth | ✅ | ✅ |
| Dashboard | ✅ | ✅ |
| Transactions | ✅ | ✅ |
| Imports | ✅ | ✅ |
| Forecasts | ✅ | ✅ |
| Reports | ✅ | ✅ |
| Tasks | ✅ | ✅ |
| Notifications | ✅ | Partial |
| Business Guide | ✅ | Partial |
| AI Accountant | ✅ | Partial |

## Optional AI Agents

Maven profiles run documentation traceability agents (requirements ↔ tests) — development tooling, not production.

```
com.flowiq.agents.traceability
```

## Local Execution

```bash
# Contract (requires running backend + PostgreSQL)
cd flowiq-automation
mvn test -Pcontract -Denv=local

# API smoke against stage
mvn test -Papi-smoke -Denv=stage
```

## Related

- [cicd-architecture.md](cicd-architecture.md)
- [test-architecture.md](test-architecture.md)
- `flowiq-automation/docs/CI-CD.md`
- `flowiq-automation/docs/qa/TRACEABILITY_MATRIX.md`
