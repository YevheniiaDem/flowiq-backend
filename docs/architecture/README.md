# FlowIQ Architecture Documentation

**As-built:** 2026-06-28  
**Repositories:** `flowiq-backend`, `flowiq-frontend`, `flowiq-automation`

This folder is the **single source of truth** for FlowIQ software architecture. Module-specific docs under `docs/modules/`, `docs/api/`, and `docs/deployment/` link here rather than duplicating diagrams.

---

## Quick Navigation

| Topic | Document |
|-------|----------|
| **High-level overview** | [system-overview.md](system-overview.md) |
| **C4 — Context (L1)** | [c4/c4-context.md](c4/c4-context.md) |
| **C4 — Containers (L2)** | [c4/c4-container.md](c4/c4-container.md) |
| **C4 — Components (L3)** | [c4/c4-component.md](c4/c4-component.md) |
| **Module dependencies** | [module-dependencies.md](module-dependencies.md) |
| **Backend** | [backend-architecture.md](backend-architecture.md) |
| **Frontend** | [frontend-architecture.md](frontend-architecture.md) |
| **Automation / QA repo** | [automation-architecture.md](automation-architecture.md) |
| **Database** | [database-architecture.md](database-architecture.md) |
| **ER diagram** | [database-er-diagram.md](database-er-diagram.md) |
| **AI / intelligence** | [ai-architecture.md](ai-architecture.md) |
| **Integrations (CSV, CORS)** | [integration-architecture.md](integration-architecture.md) |
| **Notification preferences** | [NOTIFICATION_ARCHITECTURE.md](NOTIFICATION_ARCHITECTURE.md) |
| **Profile & sessions** | [PROFILE_ARCHITECTURE.md](PROFILE_ARCHITECTURE.md) |
| **Request flow map** | [REQUEST_FLOW_MAP.md](REQUEST_FLOW_MAP.md) |
| **Component catalog** | [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) |

---

## Process Flows

| Flow | Document |
|------|----------|
| Authentication & JWT refresh | [flows/authentication-flow.md](flows/authentication-flow.md) |
| Notifications (generation → UI) | [flows/notification-flow.md](flows/notification-flow.md) |
| CSV import | [flows/import-flow.md](flows/import-flow.md) |
| Forecast Center | [flows/forecast-flow.md](flows/forecast-flow.md) |
| AI / rule engines | [flows/ai-flow.md](flows/ai-flow.md) |
| Report generation | [flows/reporting-flow.md](flows/reporting-flow.md) |

---

## Operations & Quality

| Topic | Document |
|-------|----------|
| **Deployment topology** | [deployment-architecture.md](deployment-architecture.md) |
| **CI/CD (all repos)** | [cicd-architecture.md](cicd-architecture.md) |
| **Test architecture** | [test-architecture.md](test-architecture.md) |

---

## Architecture Decision Records

| ADR | Title |
|-----|-------|
| [001](adr/001-pluggable-ai-providers.md) | Pluggable AI providers |
| [002](adr/002-transaction-seed-strategy.md) | Transaction seed strategy |
| [003](adr/003-ai-quality-factory.md) | AI quality factory |
| [004](adr/004-postgresql-selection.md) | PostgreSQL selection |
| [005](adr/005-flyway-selection.md) | Flyway selection |
| [006](adr/006-jwt-authentication-strategy.md) | JWT authentication |
| [007](adr/007-layered-architecture.md) | Layered architecture |
| [008](adr/008-frontend-architecture.md) | Frontend architecture |

---

## Related Product Docs

- [SRS](../product/SRS.md) — requirements (as-built)
- [Vision](../product/vision.md)
- [Roadmap](../product/roadmap.md)
