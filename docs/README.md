# FlowIQ Documentation

Production-grade technical and product documentation for the FlowIQ platform.

FlowIQ is a financial operations platform for Ukrainian **FOP (sole proprietor)** entrepreneurs. It combines transaction management, forecasting, task automation, tax monitoring, AI-assisted insights, and a regulatory knowledge base.

## Repositories

| Repository | Stack | Purpose |
|------------|-------|---------|
| `flowiq-backend` | Spring Boot 3.5, Java 17, PostgreSQL | REST API, business logic, schedulers |
| `flowiq-frontend` | Next.js 16, React 19, TypeScript | Web application UI |

## Quick Start

- [Local Setup](deployment/local-setup.md)
- [System Overview](architecture/system-overview.md)
- [API Overview](api/openapi-overview.md)
- [Documentation Index](index.md)

## Audience

| Audience | Start Here |
|----------|------------|
| New developers | [System Overview](architecture/system-overview.md) → [Backend Architecture](architecture/backend-architecture.md) → [Frontend Architecture](architecture/frontend-architecture.md) |
| QA engineers | [Test Strategy](qa/test-strategy.md) → [Critical User Flows](qa/critical-user-flows.md) |
| DevOps | [Docker](deployment/docker.md) → [Environments](deployment/environments.md) |
| Product / investors | [Vision](product/vision.md) → [Business Requirements](product/business-requirements.md) |

## Coverage Report

See [Documentation Coverage Report](COVERAGE-REPORT.md) for module-level completeness and known gaps.

## Living Documentation

This documentation is generated from the actual codebase implementation (controllers, entities, migrations, React routes). When code changes, update the corresponding doc section.

**Last updated:** 2026-06-11
