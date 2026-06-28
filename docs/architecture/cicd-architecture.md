# CI/CD Architecture

**As-built:** 2026-06-28  
**Scope:** All three FlowIQ repositories — build, test, and quality gates

> Operational runbooks: [deployment/ci-cd-as-built.md](../deployment/ci-cd-as-built.md), `flowiq-automation/docs/CI-CD.md`

## Multi-Repository CI Topology

```mermaid
flowchart TB
    subgraph Repos["GitHub Repositories"]
        BE_REPO[flowiq-backend]
        FE_REPO[flowiq-frontend]
        AUTO_REPO[flowiq-automation]
    end

    subgraph BackendCI["backend-ci.yml"]
        BE_MV[Maven verify]
        BE_TEST[446+ unit/integration tests]
        BE_JACOCO[JaCoCo artifact]
    end

    subgraph FrontendCI["frontend-ci.yml"]
        FE_LINT[ESLint]
        FE_BUILD[next build]
    end

    subgraph AutomationCI["flowiq-automation workflows"]
        PR_VAL[pr-validation.yml]
        NIGHTLY[nightly-regression.yml]
        API_SM[api-smoke.yml dispatch]
        UI_SM[ui-smoke.yml dispatch]
    end

    BE_REPO --> BackendCI
    FE_REPO --> FrontendCI
    AUTO_REPO --> AutomationCI

    PR_VAL -->|checkout| BE_REPO
    NIGHTLY -->|checkout| BE_REPO & FE_REPO
```

## Trigger Matrix

| Workflow | Repository | Trigger | Purpose |
|----------|------------|---------|---------|
| `backend-ci.yml` | backend | PR + push `main` | Compile, test, JaCoCo |
| `frontend-ci.yml` | frontend | PR + push `main` | Lint, build |
| `pr-validation.yml` | automation | PR + push `main`/`develop` | Backend unit + contract |
| `nightly-regression.yml` | automation | Cron 03:00 UTC + manual | Full stack regression |
| `api-smoke.yml` | automation | Manual dispatch | Stage/dev API smoke |
| `ui-smoke.yml` | automation | Manual dispatch | Stage/dev UI smoke |

## PR Validation Flow

```mermaid
sequenceDiagram
    participant Dev as Developer
    participant GH as GitHub
    participant BE as backend-ci
    participant FE as frontend-ci
    participant AUTO as pr-validation

    Dev->>GH: Open PR
    par Backend repo
        GH->>BE: backend-ci.yml
        BE->>BE: ./mvnw clean verify
        BE->>GH: Backend Tests check
    and Frontend repo
        GH->>FE: frontend-ci.yml
        FE->>FE: npm ci, lint, build
        FE->>GH: Job status
    and Automation repo
        GH->>AUTO: pr-validation.yml
        AUTO->>AUTO: unit-tests + contract-tests
        AUTO->>GH: Gate status
    end
```

## Backend CI Pipeline

```mermaid
flowchart LR
    A[checkout] --> B[Java 17 Temurin]
    B --> C[chmod mvnw]
    C --> D[mvn clean verify -B]
    D --> E[Publish Surefire XML]
    D --> F[Upload JaCoCo artifact]
    D --> G[Upload Surefire reports]

    D -.-> H["SPRING_DOCKER_COMPOSE_ENABLED=false"]
```

| Step | Detail |
|------|--------|
| Tests | Unit, controller (`@WebMvcTest`), integration (Testcontainers PostgreSQL) |
| Coverage | JaCoCo ~81% line coverage (informational gate) |
| Timeout | 20 minutes |

## Frontend CI Pipeline

```mermaid
flowchart LR
    A[checkout] --> B[Node 20]
    B --> C[npm ci]
    C --> D[npm run lint]
    D --> E[npm run build]
    E -.-> F["NEXT_PUBLIC_API_URL=localhost:8080/api"]
```

**Gap:** Vitest exists locally but is **not** in CI.

## Nightly Regression Pipeline

```mermaid
flowchart TB
    CRON[03:00 UTC] --> BUILD[build-environment]
    BUILD --> PAR[Parallel test jobs]
    PAR --> SMOKE[smoke]
    PAR --> API[api-regression]
    PAR --> UI[ui-regression]
    PAR --> CONTRACT[contract]
    SMOKE & API & UI & CONTRACT --> TD[teardown]
    TD --> ALLURE[Allure report + flaky detection]
```

Docker stack: backend + frontend + PostgreSQL built from checked-out repos.

## Quality Gates

| Gate | Backend CI | Frontend CI | Automation PR | Blocks merge |
|------|------------|-------------|---------------|--------------|
| Compile | ✅ | ✅ | ✅ | Yes |
| Unit tests | ✅ | — | ✅ (backend unit) | Yes |
| Integration tests | ✅ (in verify) | — | — | Yes |
| Contract tests | — | — | ✅ | Yes (automation repo) |
| Lint | — | ✅ | — | Yes |
| TypeScript | — | ✅ via build | — | Yes |
| JaCoCo threshold | — | — | — | No |
| E2E nightly | — | — | ✅ (scheduled) | No |
| CVE scan | ❌ | ❌ | ❌ | — |

## CD (Deployment) — Not Implemented

```mermaid
flowchart LR
    CI[Green CI] -.->|Manual| DEPLOY[Deploy]
    DEPLOY --> VERCEL[Vercel frontend]
    DEPLOY --> JAR[Backend JAR/container]
    DEPLOY --> PG[PostgreSQL + Flyway]
```

No GitHub Actions deploy workflow. See [CI/CD Evolution Plan](../deployment/CI_CD_EVOLUTION_PLAN.md).

## Secrets & Variables (Automation)

| Secret / variable | Used by |
|-------------------|---------|
| `BACKEND_REPOSITORY` | Cross-repo checkout |
| `TEST_USER_EMAIL`, `TEST_USER_PASSWORD` | Smoke, nightly |
| `GH_PAT` | Private backend checkout (optional) |

## Artifact Retention

| Artifact | Retention |
|----------|-----------|
| JaCoCo (backend CI) | 30 days |
| Surefire reports | 14 days |
| Allure (nightly/smoke) | 14 days |
| PR contract logs | 7 days |

## Related

- [test-architecture.md](test-architecture.md)
- [automation-architecture.md](automation-architecture.md)
- [deployment-architecture.md](deployment-architecture.md)
