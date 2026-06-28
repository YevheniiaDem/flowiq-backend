# Test Architecture

**As-built:** 2026-06-28  
**Scope:** Testing strategy across `flowiq-backend`, `flowiq-frontend`, `flowiq-automation`

## Test Pyramid (As-Built)

```mermaid
flowchart TB
    subgraph Pyramid["Test Pyramid"]
        E2E["E2E Journeys<br/>flowiq-automation<br/>11 classes"]
        INT["Integration<br/>Testcontainers + @WebMvcTest<br/>flowiq-backend"]
        UNIT["Unit Tests<br/>446 tests backend<br/>Vitest frontend local only"]
        CONTRACT["Contract<br/>JSON Schema vs OpenAPI<br/>flowiq-automation"]
    end

    E2E --> INT --> UNIT
    CONTRACT --> INT
```

| Layer | Count / status | Repository |
|-------|----------------|--------------|
| **Unit** | 446 tests, 72 classes, ~81% line JaCoCo | `flowiq-backend` |
| **Controller** | 15 controller test classes (MockMvc) | `flowiq-backend` |
| **Integration** | Repository, security, Flyway tests | `flowiq-backend` |
| **Contract** | OpenAPI schema validation | `flowiq-automation` |
| **API regression** | Rest Assured suites | `flowiq-automation` |
| **UI / E2E** | Playwright Java | `flowiq-automation` |
| **Frontend unit** | Vitest (local) | `flowiq-frontend` — **not in CI** |

## Backend Test Structure

```mermaid
flowchart TB
    subgraph BackendTests["flowiq-backend/src/test"]
        UNIT_PKG[unit/** — services, engines, security]
        CTRL[controller/** — MockMvc all 15 controllers]
        INT_REPO[integration/repository/**]
        INT_SEC[integration/security/**]
        INT_MIG[integration/migration/Flyway]
        SUPPORT[unit/support/ControllerTestSupport]
    end

    subgraph Infra
        H2[H2 or Testcontainers PostgreSQL]
        MOCK[Mockito + AssertJ]
        MVC[MockMvc + @WebMvcTest]
    end

    UNIT_PKG & CTRL --> MOCK
    INT_REPO & INT_SEC & INT_MIG --> H2
    CTRL --> MVC & SUPPORT
```

### Test configuration

| File | Purpose |
|------|---------|
| `application-test.properties` | H2/Testcontainers, disable compose, demo seed off |
| `AbstractPostgresIntegrationTest` | Shared Testcontainers base |
| `ControllerTestSupport` | JWT + MockMvc helpers |

### Surefire inclusion

`pom.xml`: `**/*Test.java`, `**/*Tests.java` — includes integration and application context tests.

## Automation Test Structure

```mermaid
flowchart LR
    subgraph Packages
        API[api/ Rest Assured]
        UI[ui/ Playwright]
        E2E[e2e/ journeys]
        CONTRACT[contract/ schema]
    end

    subgraph Profiles
        P1[-Papi-smoke]
        P2[-Papi-regression]
        P3[-Pui-smoke]
        P4[-Pcontract]
        P5[-Pe2e]
    end

    API --> P1 & P2
    UI --> P3
    CONTRACT --> P4
    E2E --> P5
```

## CI Test Execution Map

```mermaid
flowchart TB
    subgraph OnEveryPR
        BE[mvn verify — all backend tests]
        FE[npm lint + build]
        AUTO_PR[automation: unit + contract]
    end

    subgraph Nightly
        FULL[Full Docker stack]
        FULL --> SMOKE & API_R & UI_R & CONTRACT_R
    end

    subgraph Manual
        API_S[api-smoke stage/dev]
        UI_S[ui-smoke stage/dev]
    end
```

## Coverage by Domain

| Domain | Backend unit | Controller | Integration | Automation API | Automation E2E |
|--------|-------------|------------|-------------|----------------|----------------|
| Auth | ✅ | ✅ | ✅ security | ✅ | ✅ |
| Transactions | ✅ | ✅ | ✅ repo | ✅ | ✅ |
| Imports | ✅ | ✅ | — | ✅ | ✅ |
| Dashboard | ✅ | ✅ | — | ✅ | ✅ |
| Analytics | ✅ | ✅ | — | ✅ | Partial |
| Forecasts | ✅ | ✅ | — | ✅ | ✅ |
| AI Accountant | ✅ | ✅ | — | ✅ | Partial |
| Chat | ✅ | ✅ | — | Partial | — |
| Tasks | ✅ | ✅ | ✅ repo | ✅ | ✅ |
| Notifications | ✅ | ✅ | ✅ repo | ✅ | Partial |
| Reports | ✅ | ✅ | — | ✅ | ✅ |
| Business Guide | ✅ | ✅ | — | ✅ | Partial |
| Profile | ✅ | ✅ | — | Partial | — |

Detail: [BACKEND_TEST_COVERAGE_REPORT.md](../qa/BACKEND_TEST_COVERAGE_REPORT.md), `flowiq-automation/docs/qa/TRACEABILITY_MATRIX.md`.

## Test Data Strategy

| Environment | User | Data |
|-------------|------|------|
| Backend unit/integration | Mock / H2 / Testcontainers | Programmatic fixtures |
| Automation local/CI | `demo@flowiq.ai` or `TEST_USER_*` | Demo seed + Docker seed scripts |
| Nightly Docker | Seeded stack | `ci-up.sh` |

## Reporting & Artifacts

| Tool | Output | Where |
|------|--------|-------|
| JaCoCo | HTML coverage | Backend CI artifact |
| Surefire | JUnit XML | GitHub Checks |
| Allure | HTML report | Nightly/smoke automation |
| EnricoMi action | PR test comments | Backend CI |

## Gaps & Evolution

| Gap | Priority |
|-----|----------|
| Frontend Vitest in CI | Medium |
| JaCoCo minimum threshold gate | Low |
| Dependency/CVE scanning | Medium |
| Performance/load tests | Low |
| Visual regression | Low |

## Related

- [automation-architecture.md](automation-architecture.md)
- [cicd-architecture.md](cicd-architecture.md)
- [Test Strategy](../qa/test-strategy.md) — operational checklist
- [Critical User Flows](../qa/critical-user-flows.md)
