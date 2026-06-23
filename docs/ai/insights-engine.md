# Insights Engine

Rule-based insight generation across modules. **Source of truth:** backend Java services listed below.

## Dashboard Insights

| Attribute | Value |
|-----------|-------|
| **File** | `src/main/java/com/flowiq/service/DashboardService.java` |
| **Type** | Service (inline rule methods) |
| **Called by** | `DashboardController.getInsights()` → `GET /api/dashboard/insights` |
| **Frontend** | `flowiq-frontend/src/services/dashboard.service.ts` → `getInsights()` |
| **Production** | Yes |
| **Real calls** | Yes — compares current vs previous month aggregates from `TransactionRepository` |
| **Future hook** | No — does not use `TransactionInsightService` or `AIInsightProvider` |

`DashboardService.getInsights()` applies threshold rules on month-over-month revenue/expense growth and profit sign. It does **not** delegate to `TransactionInsightService` (that service has **no callers** in the codebase).

Rules implemented today (`getInsights()`):

- `expenseGrowth > revenueGrowth && expenseGrowth > 10` → warning insight
- `revenueGrowth > 0` → success insight
- `profit > 0` → info insight
- empty list → default “getting started” insight

Related endpoints on the same service: `getBusinessHealth()`, `getAISummary()` — separate inline formulas, not shared with AI Accountant health.

## AI Accountant

| Attribute | Value |
|-----------|-------|
| **Engine file** | `src/main/java/com/flowiq/aiaccountant/AIRecommendationEngine.java` |
| **Type** | Engine (`@Component`) |
| **Service file** | `src/main/java/com/flowiq/service/AIAccountantService.java` |
| **Called by** | `AIAccountantController` → `/api/ai-accountant/*` |
| **Frontend** | `flowiq-frontend/src/features/ai-accountant/services/aiAccountantService.ts` |
| **Production** | Yes |
| **Real calls** | Yes — `recommendationEngine.generate(snapshot)` in `getRecommendations()` |
| **Future hook** | `List<AIInsightProvider>` merged after rules in `getRecommendations()` and tried first in `chat()` — **no implementations registered** |

`AIAccountantService.getForecasts()` uses **inline** `buildForecast()` math on `FinancialSnapshot` — it does **not** call `ForecastEngine` or `ForecastService`.

Endpoints:

- `GET /api/ai-accountant/recommendations` — `AIRecommendationEngine` + optional providers
- `GET /api/ai-accountant/tax-advisor` — delegates to `AnalyticsService.getFopInsights()`
- `GET /api/ai-accountant/forecasts` — inline horizon projection (3/6/12 months)
- `POST /api/ai-accountant/chat` — optional `AIInsightProvider`, else keyword templates

## Forecast Insights

| Attribute | Value |
|-----------|-------|
| **Provider file** | `src/main/java/com/flowiq/forecasts/provider/RuleBasedForecastProvider.java` |
| **Engine file** | `src/main/java/com/flowiq/forecasts/engine/ForecastEngine.java` |
| **Orchestrator** | `src/main/java/com/flowiq/forecasts/service/ForecastService.java` |
| **Type** | Provider + Engine |
| **Called by** | `ForecastController` → `/api/forecasts/*`; dashboard widget via `DashboardController.getForecastSnapshot()` → `ForecastService.getSnapshot()` |
| **Frontend** | `flowiq-frontend/src/features/forecasts/services/forecast.service.ts` |
| **Production** | Yes |
| **Real calls** | Yes — insights/warnings on `GET /api/forecasts/summary` only |

Narrative insights come from `RuleBasedForecastProvider.generateInsights()`; warnings from `generateWarnings()`. Additional `ForecastProvider` beans are merged **only** into summary insights (not warnings).

## Notification Insights

| Attribute | Value |
|-----------|-------|
| **File** | `src/main/java/com/flowiq/notifications/service/NotificationRuleEngine.java` |
| **Type** | Engine (`@Service`) |
| **Called by** | `NotificationScheduler`, import/report event hooks |
| **Production** | Yes |
| **Real calls** | Yes — persists via `NotificationGeneratorService` |

## TransactionInsightService (future hook only)

| Attribute | Value |
|-----------|-------|
| **File** | `src/main/java/com/flowiq/service/TransactionInsightService.java` |
| **Type** | Service (data preparation) |
| **Called by** | **None** — no production or test references outside its own class |
| **Production** | Bean registered, **never invoked** |
| **Future hook** | Yes — `buildAnalysisContext()` aggregates transactions for a date range |

## Extension Points

| Interface | Wired in | Invoked today |
|-----------|----------|---------------|
| `AIInsightProvider` | `AIAccountantService` | Loop runs; **zero beans** |
| `AnalyticsInsightProvider` | `AnalyticsService` constructor | Field stored; **never read** |
| `CategorizationProvider` | `CategorizationEngine` | Loop runs; **zero beans** |
| `ForecastProvider` | `ForecastService` | `RuleBasedForecastProvider` only |

## Related

- [AI Architecture](../architecture/ai-architecture.md)
- [AI Documentation Audit Report](../architecture/AI_DOCUMENTATION_AUDIT_REPORT.md)
