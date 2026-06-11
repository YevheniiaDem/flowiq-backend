# FlowIQ Vision

## What Is FlowIQ?

FlowIQ is a **financial intelligence platform** for Ukrainian individual entrepreneurs (ФОП). It helps business owners understand cash flow, forecast revenue and taxes, stay compliant with FOP regulations, and act on deadlines — without hiring a full accounting team on day one.

The product combines:

- **Transaction ledger** with CSV bank import
- **Dashboard** with AI-style business insights
- **Forecast Center 2.0** — revenue, expenses, profit, tax, and FOP limit projections
- **Tasks & Deadlines Center** — auto-generated compliance tasks
- **Notification Center** — tax, FOP limit, and financial alerts
- **Business Guide** — bilingual knowledge base for FOP law and taxes
- **AI Accountant** — rule-based recommendations (LLM-ready architecture)
- **Reports** — PDF/Excel/CSV export
- **Analytics** — trends and FOP-specific metrics

## Target Audience

| Segment | Need |
|---------|------|
| **FOP entrepreneurs** (IT, services, trade) | Simple tax compliance, income limits, deadlines |
| **Growing FOPs** (Group 2 → 3) | Forecasting, VAT transition, hiring |
| **Accountants / consultants** | Client overview, exportable reports |
| **Future admins** | Multi-tenant operations (not yet implemented) |

Primary market: **Ukraine**, Ukrainian and English UI (`uk` default).

## Value Proposition

1. **One place for money + compliance** — transactions, taxes, and deadlines in a single dashboard.
2. **Proactive, not reactive** — forecasts and notifications before limits are exceeded.
3. **Localized for FOP** — unified tax groups, ЄСВ, ПДВ, KVED, quarterly declarations.
4. **AI-ready** — extensible provider interfaces; current logic is deterministic and auditable.
5. **Self-service knowledge** — Business Guide answers “Які податки платить ФОП 3 групи?” without external search.

## Business Goals

| Goal | Implementation |
|------|----------------|
| Reduce tax compliance anxiety | Tasks, notifications, Business Guide |
| Increase retention via daily utility | Dashboard, forecasts, task calendar |
| Enable upsell to accountant tools | Reports, analytics, AI Accountant |
| Prepare for LLM monetization | `ForecastProvider`, `KnowledgeProvider`, `AIInsightProvider` |
| Support demo-led acquisition | `demo@flowiq.ai` / `demo123` seeded data |

## Product Principles

- **Bilingual by default** — UK/EN via `X-App-Language` header and frontend i18n.
- **JWT-secured API** — stateless auth for SPA deployment.
- **Feature modules** — forecasts, tasks, notifications, knowledge as isolated packages.
- **No silent mock in production paths** — knowledge articles live in PostgreSQL; some UI still uses local mock data (see [Coverage Report](../COVERAGE-REPORT.md)).

## Related Documents

- [Business Requirements](business-requirements.md)
- [User Personas](user-personas.md)
- [Roadmap](roadmap.md)
- [System Overview](../architecture/system-overview.md)
