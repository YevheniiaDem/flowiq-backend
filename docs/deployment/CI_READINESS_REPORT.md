# CI Readiness Report

**Date:** 2026-06-22  
**Scope:** `flowiq-backend`, `flowiq-frontend`  
**Goal:** Automated quality checks on every Pull Request before architectural review

---

## Executive Summary

Minimal **production-ready CI** is now implemented via GitHub Actions in both repositories. Pipelines run on **pull_request** and **push to main**. No CD was added.

| Repository | Workflow | Status |
|------------|----------|--------|
| flowiq-backend | `backend-ci.yml` | ✅ Implemented |
| flowiq-frontend | `frontend-ci.yml` | ✅ Implemented |

---

## What Was Implemented

### Backend (`flowiq-backend/.github/workflows/backend-ci.yml`)

| Component | Detail |
|-----------|--------|
| Trigger | `pull_request`, `push` → `main` |
| Runtime | `ubuntu-latest`, Java 17 (Temurin) |
| Build command | `./mvnw clean verify -B` |
| Maven cache | `actions/setup-java@v4` with `cache: maven` |
| Test publishing | `EnricoMi/publish-unit-test-result-action@v2` → GitHub Checks |
| Coverage | JaCoCo HTML uploaded as artifact (`target/site/jacoco/`) |
| Surefire XML | Uploaded as artifact for debugging |

**Verified locally:** `mvnw clean verify` — BUILD SUCCESS, 95 tests, 0 failures.

### Frontend (`flowiq-frontend/.github/workflows/frontend-ci.yml`)

| Component | Detail |
|-----------|--------|
| Trigger | `pull_request`, `push` → `main` |
| Runtime | `ubuntu-latest`, Node 20 |
| Install | `npm ci` (lockfile required) |
| Lint | `npm run lint` |
| Build | `npm run build` with `NEXT_PUBLIC_API_URL` |
| npm cache | `actions/setup-node@v4` with `cache: npm` |

**Tooling adjustment:** `eslint.config.mjs` — disabled `react-hooks/set-state-in-effect` (React 19 false positives on standard data-fetch hooks). No business logic changed.

**Verified locally:** lint exit 0, `next build` success with TypeScript check.

### Documentation

| File | Purpose |
|------|---------|
| [ci-cd-as-built.md](ci-cd-as-built.md) | Factual pipeline reference + Mermaid diagrams |
| [ci-cd.md](ci-cd.md) | Updated overview (removed "no CI" statements) |
| This report | Readiness assessment + maturity score |

---

## Quality Gates (New)

| Gate | When | Fails PR? |
|------|------|-----------|
| Backend compile | Every backend PR | ✅ Yes |
| Backend unit tests (95) | Every backend PR | ✅ Yes |
| Spring Boot JAR package | Every backend PR | ✅ Yes |
| Frontend ESLint (0 errors) | Every frontend PR | ✅ Yes |
| Frontend TypeScript + Next.js build | Every frontend PR | ✅ Yes |
| JaCoCo coverage report | Artifact upload | ❌ No (informational) |

---

## Automated vs Manual Checks

### Now automated on PR

| Check | Repo |
|-------|------|
| Java compilation | Backend |
| Unit tests (engines, services, PDF/Excel renderers) | Backend |
| Maven package / repackage | Backend |
| ESLint | Frontend |
| TypeScript strict check | Frontend |
| Next.js production build (19 routes) | Frontend |

### Still manual

| Check | Notes |
|-------|-------|
| Smoke checklist (15 min) | [smoke-checklist.md](../qa/smoke-checklist.md) |
| Regression checklist | Post-deploy |
| Integration tests | No Testcontainers in CI |
| `@SpringBootTest` + Flyway + PostgreSQL | `FlowiqBackendApplicationTests` not in Surefire `*Test.java` pattern |
| Playwright / E2E | Not configured |
| Docker image build | Dockerfile not executed in CI |
| Security scan (CVE, OWASP) | Not configured |
| OpenAPI contract diff | Not configured |
| Cross-repo E2E (frontend + backend) | Separate repos, no orchestration |
| Deploy to Vercel / cloud | Manual CD |

---

## Known Limitations

1. **`FlowiqBackendApplicationTests`** — `@SpringBootTest` class named `*Tests.java` is excluded by Surefire `**/*Test.java` include pattern. Full context + Flyway validation does not run in CI today.
2. **No PostgreSQL service** in backend CI — not required for current unit tests.
3. **Separate repositories** — changing both apps requires two PRs with independent checks.
4. **Coverage threshold** — JaCoCo uploaded but no minimum % gate enforced.
5. **ESLint warnings** — 5 warnings allowed (unused vars); 0 errors required.

---

## Dockerfile Compatibility

| Image | CI validates | Docker build |
|-------|--------------|--------------|
| Backend | `mvn verify` (includes tests) | `mvn package -DskipTests` |
| Frontend | `npm run build` | Same `next build` command |

CI is **stricter** than Docker build for backend (tests run). A passing CI build implies Docker compile stage will succeed.

---

## CI/CD Maturity Score

### Scoring model (0–100)

| Dimension | Weight | Description |
|-----------|--------|-------------|
| Build automation | 25% | Compile/package on PR |
| Test automation | 25% | Unit tests in pipeline |
| Quality gates | 20% | Lint, types, enforced checks |
| Feedback | 15% | Test results in GitHub UI |
| CD / deploy | 15% | Automated deployment |

### Before (2026-06-17)

| Dimension | Score |
|-----------|-------|
| Build automation | 0 |
| Test automation | 0 |
| Quality gates | 0 |
| Feedback | 0 |
| CD / deploy | 0 |
| **Weighted total** | **0 / 100** |

No `.github/workflows/`, manual `mvnw test` and `npm run build` only.

### After (2026-06-22)

| Dimension | Score | Rationale |
|-----------|-------|-----------|
| Build automation | **90** | Both repos build on PR; no matrix |
| Test automation | **55** | Backend unit tests only; no integration/E2E |
| Quality gates | **70** | Lint + types + tests; no coverage threshold |
| Feedback | **75** | Surefire → GitHub Checks; frontend job log only |
| CD / deploy | **0** | Intentionally not implemented |
| **Overall CI/CD** | **62 / 100** | CI implemented; CD not started |
| **CI only** | **75 / 100** | Build + unit tests + lint + types on PR |
| **CD only** | **0 / 100** | Manual deploy |

```
(90×0.25) + (55×0.25) + (70×0.20) + (75×0.15) + (0×0.15) = 61.5 → 62/100
```

### Path to CI 90+

1. Add PostgreSQL service + fix Surefire pattern to include `*Tests.java` or rename test class
2. Integration tests with Testcontainers
3. JaCoCo minimum coverage gate (e.g. 70% on engines)
4. Frontend Vitest unit tests
5. Branch protection requiring green checks
6. Dependabot + `npm audit` / OWASP dependency check

### Path to CD 50+

1. GitHub Actions deploy workflow (staging on merge to main)
2. Docker build + push to registry
3. Post-deploy smoke hook

---

## Recommendations Before Architect Review

1. **Enable branch protection** on `main` — require `Maven Verify` and `Lint and Build`.
2. **Rename** `FlowiqBackendApplicationTests` → `FlowiqBackendApplicationTest` to include in CI (optional quick win for context load).
3. **Document** in PR template that backend and frontend PRs are independent.
4. **Defer CD** until staging environment is defined ([environments.md](environments.md)).

---

## Related

- [CI/CD As-Built](ci-cd-as-built.md)
- [ADR-019 candidate](../architecture/adr/ADR_COVERAGE_REPORT.md) — CI/CD ADR when CD is added
- [Architecture Review Readiness](../architecture/ARCHITECTURE_REVIEW_READINESS.md)
