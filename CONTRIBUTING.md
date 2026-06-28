# Contributing to FlowIQ

Thank you for your interest in contributing to FlowIQ. This document covers the workflow for all three repositories in the platform.

**Start here:** [Developer Handbook](docs/DEVELOPER_HANDBOOK.md) — setup, architecture, testing, and release process.

---

## Repositories

| Repository | Default branch | Focus |
|------------|----------------|-------|
| [flowiq-backend](https://github.com/YevheniiaDem/flowiq-backend) | `main` | API, business logic, database |
| [flowiq-frontend](https://github.com/YevheniiaDem/flowiq-frontend) | `master` | Web UI |
| [flowiq-automation](https://github.com/YevheniiaDem/flowiq-automation) | `main` | API/UI/E2E tests |

Documentation hub (backend repo): [docs/index.md](docs/index.md)

---

## How to contribute

### 1. Find or discuss the change

- Check [open issues](https://github.com/YevheniiaDem/flowiq-backend/issues) or open a new one for non-trivial work.
- For product scope questions, see [docs/product/SRS.md](docs/product/SRS.md) and [docs/product/roadmap.md](docs/product/roadmap.md).
- Do not implement features marked **Future / Not Implemented** in the SRS unless agreed with maintainers.

### 2. Fork and branch

```bash
git checkout main          # or master for frontend
git pull origin main
git checkout -b feature/short-description
```

Branch naming:

| Prefix | Use for |
|--------|---------|
| `feature/` | New functionality |
| `fix/` | Bug fixes |
| `docs/` | Documentation only |
| `test/` | Test coverage |
| `refactor/` | Code structure (no behavior change) |

### 3. Develop locally

Follow [Local setup](docs/DEVELOPER_HANDBOOK.md#2-local-setup) in the developer handbook.

Minimum verification before pushing:

| Repository | Command |
|------------|---------|
| **Backend** | `./mvnw clean verify` |
| **Frontend** | `npm ci && npm run lint && npm run build` |
| **Automation** | `mvn clean compile test-compile` (+ contract tests if API changed) |

### 4. Write tests

| Change | Expected tests |
|--------|----------------|
| Backend service/controller | JUnit tests in `src/test/java/` |
| Backend bug fix | Regression test covering the fix |
| Frontend logic | Vitest test (encouraged; not CI-gated yet) |
| API contract change | Update automation contract schemas |

See [Testing guide](docs/DEVELOPER_HANDBOOK.md#12-testing-guide).

### 5. Update documentation

- **API changes:** OpenAPI annotations on DTOs/controllers; update [docs/api/](docs/api/) if behavior changes.
- **Schema changes:** New Flyway migration only — never edit applied migrations.
- **Architecture changes:** Update [docs/architecture/](docs/architecture/README.md).
- **Do not duplicate** — link to existing docs from the handbook.

### 6. Open a pull request

- Target the repository's **default branch** (`main` or `master`).
- Fill in the PR description: what, why, how to test.
- Link related PRs in other repos (e.g. backend + frontend API change).
- Ensure CI checks pass.

### 7. Code review

Maintainers review for:

- Correctness and alignment with [SRS](docs/product/SRS.md) as-built scope
- Tests and documentation
- Security ([SECURITY_AUDIT.md](docs/security/SECURITY_AUDIT.md))
- No secrets committed (`.env`, keys, passwords)

---

## Code style

Summary — full detail in [Developer Handbook §7](docs/DEVELOPER_HANDBOOK.md#7-code-style).

### Backend

- Java 17, Spring Boot conventions
- Jakarta Validation on request DTOs
- Lombok where already used in the module
- Match existing package and naming patterns

### Frontend

- TypeScript strict mode (via `next build`)
- ESLint must pass: `npm run lint`
- Feature-module structure under `src/features/`

### General

- Prefer small, focused PRs
- No unrelated refactors mixed with feature work
- No committed secrets or generated `target/` / `node_modules/` artifacts

---

## Database migrations

1. Add `src/main/resources/db/migration/V{n}__description.sql`.
2. Never modify migrations that have been applied in shared environments.
3. Run `./mvnw test` — integration tests use Testcontainers + Flyway.

---

## Security

- Report vulnerabilities privately to maintainers (do not open public issues for exploitable bugs).
- Never commit real credentials. Use environment variables in production.
- Read [docs/security/SECURITY_AUDIT.md](docs/security/SECURITY_AUDIT.md) before touching auth, uploads, or CORS.

---

## CI expectations

| Repo | CI workflow | Must pass |
|------|-------------|-----------|
| Backend | `backend-ci.yml` | Maven verify + tests |
| Frontend | `frontend-ci.yml` | Lint + build |
| Automation | `pr-validation.yml` | Compile + contract tests |

Cross-repo: automation PR validation may checkout backend at the same branch name.

Detail: [docs/architecture/cicd-architecture.md](docs/architecture/cicd-architecture.md)

---

## Commit messages

Use clear, imperative subjects:

```
Add refresh token rotation audit event

Fix CSV duplicate detection for PrivatBank imports
docs: update developer handbook CI section
```

Optional body: motivation, breaking changes, issue references.

---

## License

The backend repository README currently states **Proprietary — All Rights Reserved**. Contributors should confirm licensing with maintainers before assuming open-source distribution. A license file will be added when the project formally open-sources.

---

## Getting help

| Resource | Link |
|----------|------|
| Developer handbook | [docs/DEVELOPER_HANDBOOK.md](docs/DEVELOPER_HANDBOOK.md) |
| Architecture | [docs/architecture/README.md](docs/architecture/README.md) |
| API reference | http://localhost:8080/swagger-ui.html (local dev) |
| Troubleshooting | [Handbook §15](docs/DEVELOPER_HANDBOOK.md#15-troubleshooting) |

---

*Thank you for helping improve FlowIQ.*
