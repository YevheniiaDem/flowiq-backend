# ADR-002: Transaction Seed Strategy

**Status:** Accepted  
**Date:** 2026-06-17  
**Context:** FlowIQ financial modules (dashboard, analytics, forecasts, reports, AI Accountant, chat, tasks) require transaction history to produce meaningful output. New users have empty `transactions` tables after registration.

## Decision

Use **`TransactionSeedService`** with an **auto-seed on first access** pattern:

- On `seedIfEmpty(User user)`, if the user has **no transactions**, insert **6 months** of synthetic `REVENUE` and `EXPENSE` rows into PostgreSQL.
- If the user already has data, call `ensureSixMonthHistory()` to backfill months with zero revenue only.
- Seed is triggered from domain services (not from a dedicated onboarding endpoint).

**Call sites today:** `DashboardService`, `AnalyticsService`, `ForecastService`, `ReportsService`, `ChatService`, `AIAccountantService`, `TaskService`.

**Not seeded by:** `TransactionService` (CRUD), `ImportService`, `NotificationService` (read path).

## Why Auto-Seed

| Driver | Explanation |
|--------|-------------|
| **Demo UX** | Dashboard, forecasts, and AI insights are empty without history — poor first impression for MVP demos |
| **Single data model** | Demo and real data share the same `transactions` table and code paths — no parallel mock layer in backend |
| **Offline sales** | `demo@flowiq.ai` can explore full product without manual CSV upload |
| **Rule engine input** | Forecasts, notifications, and tasks need ≥1 month of aggregates to run |

## Consequences

### Positive

- Instant populated experience on first login
- All modules tested against the same repository queries
- No separate "demo mode" API or in-memory store
- Seed logic is centralized in one service

### Negative

- Users cannot distinguish seeded vs imported data without UI labeling
- Seeded rows persist until manually deleted
- Hardcoded revenue targets (150k–245k UAH) may misrepresent user's actual business
- `ensureSixMonthHistory()` can add demo rows to months with zero revenue even after partial real usage

### Risks: Demo vs Production Data Mixing

| Risk | Severity | Mitigation (recommended) |
|------|----------|--------------------------|
| User believes seeded metrics are real | **High** | UI banner: "Demo data" until first import or manual transaction |
| Tax/FOP calculations based on fake revenue | **High** | Disable seed in production profile or gate behind `flowiq.features.demo-seed-enabled` |
| Audit/compliance confusion | **Medium** | Flag seeded rows (`source = SEED`) — **not implemented today** |
| Cannot reset without DELETE | **Medium** | "Clear demo data" endpoint — **not implemented today** |

## Alternatives Considered

1. **Frontend-only mock data** — rejected for backend modules; would duplicate logic and diverge from API contracts
2. **Separate demo tenant / read-only sample account** — deferred; current approach uses per-user seed
3. **Empty state UI with onboarding wizard** — rejected for MVP; higher frontend effort, weaker demo story
4. **Seed on registration only** — rejected; lazy seed defers cost until first module access
5. **In-memory seed per request** — rejected; breaks reports, schedulers, and pagination

## Production Disable Criteria

Disable or gate `TransactionSeedService` when **any** of the following apply:

| Criterion | Action |
|-----------|--------|
| Production environment (`spring.profiles.active=prod`) | Set `flowiq.features.demo-seed-enabled=false` (feature flag — **recommended, not yet in code**) |
| User completed first CSV import | Skip seed for that user |
| User created ≥1 manual transaction | Skip initial seed (already handled by `existsByUserId`) |
| Regulatory requirement for data accuracy | Mandatory disable + UI labeling |
| Paid subscription tier | No auto-seed |

**Current state:** No profile-based disable exists — seed runs in all environments.

## Implementation Notes

```java
// TransactionSeedService.seedIfEmpty — simplified flow
if (transactionRepository.existsByUserId(user.getId())) {
    ensureSixMonthHistory(user);
    return;
}
// Insert 6 months × (4 revenue + 5 expense) categories
transactionRepository.saveAll(transactions);
```

Hardcoded parameters: monthly revenue targets, expense ratio 0.62, category splits — see [Data Sources](../data-sources.md).

## Related

- [Data Sources](../data-sources.md)
- [ADR-004: PostgreSQL Selection](004-postgresql-selection.md)
- [Architecture Review Readiness](../ARCHITECTURE_REVIEW_READINESS.md)
