# FlowIQ Documentation

Production-grade technical and product documentation for the FlowIQ platform.

FlowIQ is a financial operations platform for Ukrainian **FOP (sole proprietor)** entrepreneurs — transactions, forecasting, tasks, notifications, rule-based AI insights, and a regulatory knowledge base.

## Start here

| Audience | Document |
|----------|----------|
| **All developers** | **[Developer Handbook](DEVELOPER_HANDBOOK.md)** |
| Contributors | [../CONTRIBUTING.md](../CONTRIBUTING.md) |
| Architects | [architecture/README.md](architecture/README.md) |
| Product / QA | [product/SRS.md](product/SRS.md) |
| Security | [security/SECURITY_AUDIT.md](security/SECURITY_AUDIT.md) |

## Repositories

| Repository | Stack | Purpose |
|------------|-------|---------|
| **flowiq-backend** | Spring Boot 3.5, Java 17, PostgreSQL | REST API, business logic, schedulers |
| **flowiq-frontend** | Next.js 16, React 19, TypeScript | Web application UI |
| **flowiq-automation** | TestNG, Rest Assured, Playwright | Cross-repo tests |

## Quick links

- [Local setup](deployment/local-setup.md)
- [System overview](architecture/system-overview.md)
- [API overview](api/openapi-overview.md)
- [Full index](index.md)

## Audience guide

| Audience | Path |
|----------|------|
| New developers | [DEVELOPER_HANDBOOK.md](DEVELOPER_HANDBOOK.md) → [Architecture](architecture/README.md) |
| QA engineers | [DEVELOPER_HANDBOOK.md §12](DEVELOPER_HANDBOOK.md#12-testing-guide) → [Test architecture](architecture/test-architecture.md) |
| DevOps | [DEVELOPER_HANDBOOK.md §14](DEVELOPER_HANDBOOK.md#14-deployment-guide) → [Docker](deployment/docker.md) |
| Product | [Vision](product/vision.md) → [SRS](product/SRS.md) |

## Living documentation

Documentation reflects the **as-built** codebase. When code changes, update the linked deep-dive doc (see handbook maintenance table).

**Last updated:** 2026-06-28
