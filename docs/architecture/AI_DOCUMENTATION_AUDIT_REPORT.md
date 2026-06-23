# AI Documentation Audit Report

**Date:** 2026-06-23  
**Source of truth:** `flowiq-backend` and `flowiq-frontend` Java/TypeScript code only  
**Documentation audited:** `docs/architecture/ai-*.md`, `docs/ai/*`, `docs/architecture/ARCHITECTURE_REVIEW_READINESS.md`  
**Method:** Static analysis — `grep`, controller/service trace, provider interface implementor search

---

## Executive Summary

FlowIQ’s production “AI” layer is **100% rule-based**. Five provider interfaces exist for future LLM integration; only `ForecastProvider` and `KnowledgeProvider` have active implementations. No class implements `AIInsightProvider`, `AnalyticsInsightProvider`, or `CategorizationProvider`.

**Critical code facts corrected in documentation:**

1. `TransactionInsightService` is registered as a Spring bean but has **zero callers**.
2. `DashboardService.getInsights()` uses **inline rules** — it does not use `TransactionInsightService`.
3. `AnalyticsInsightProvider` is **injected but never invoked** in `AnalyticsService`.
4. `AIAccountantService.getForecasts()` does **not** use `ForecastEngine` (separate inline math).
5. `ForecastProvider` merge applies to **summary insights only**; warnings are always rule-based.

The frontend contains **no AI engines** — only HTTP clients to backend REST APIs.

---

## Component Catalog (Backend)

| Component | File | Type | Called by | Production | Real calls | Future hook |
|-----------|------|------|-----------|------------|------------|-------------|
| `DashboardService` | `service/DashboardService.java` | Service | `DashboardController` | Yes | Yes | No |
| `AIAccountantService` | `service/AIAccountantService.java` | Service | `AIAccountantController` | Yes | Yes | `AIInsightProvider` |
| `AIRecommendationEngine` | `aiaccountant/AIRecommendationEngine.java` | Engine | `AIAccountantService` | Yes | Yes | No |
| `ForecastService` | `forecasts/service/ForecastService.java` | Service | `ForecastController`, `DashboardController` | Yes | Yes | `ForecastProvider` |
| `ForecastEngine` | `forecasts/engine/ForecastEngine.java` | Engine | `ForecastService` | Yes | Yes | No |
| `RuleBasedForecastProvider` | `forecasts/provider/RuleBasedForecastProvider.java` | Provider | `ForecastService` | Yes | Yes (summary) | No |
| `AnalyticsService` | `service/AnalyticsService.java` | Service | `AnalyticsController`, `AIAccountantService`, `ReportsService` | Yes | Yes | `AnalyticsInsightProvider` unused |
| `CategorizationEngine` | `categorization/CategorizationEngine.java` | Engine | `ImportService` | Yes | Yes | `CategorizationProvider` |
| `DefaultCategoryRules` | `categorization/DefaultCategoryRules.java` | Rules (static) | `CategorizationEngine` | Yes | Yes | No |
| `KnowledgeService` | `knowledge/service/KnowledgeService.java` | Service | `BusinessGuideController`, `DashboardController` | Yes | Yes | `KnowledgeProvider` |
| `DatabaseKnowledgeProvider` | `knowledge/provider/DatabaseKnowledgeProvider.java` | Provider | `KnowledgeService` | Yes | Yes | No |
| `ChatService` | `service/ChatService.java` | Service | `ChatController` | Yes | Yes | No |
| `NotificationRuleEngine` | `notifications/service/NotificationRuleEngine.java` | Engine | `NotificationScheduler`, events | Yes | Yes | No |
| `TaskRuleEngine` | `tasks/service/TaskRuleEngine.java` | Engine | `DailyTaskScheduler`, `TaskService` | Yes | Yes | No |
| `TransactionInsightService` | `service/TransactionInsightService.java` | Service | **None** | Bean only | **No** | **Yes** |
| `AIInsightProvider` | `aiaccountant/AIInsightProvider.java` | Interface | `AIAccountantService` | Wired | Loop empty | **Yes** |
| `AnalyticsInsightProvider` | `analytics/AnalyticsInsightProvider.java` | Interface | `AnalyticsService` | Wired | **Not invoked** | **Yes** |
| `CategorizationProvider` | `categorization/CategorizationProvider.java` | Interface | `CategorizationEngine` | Wired | Loop empty | **Yes** |
| `ForecastProvider` | `forecasts/provider/ForecastProvider.java` | Interface | `ForecastService` | Wired | `RuleBasedForecastProvider` | Partial |
| `KnowledgeProvider` | `knowledge/provider/KnowledgeProvider.java` | Interface | `KnowledgeService` | Wired | `DatabaseKnowledgeProvider` | Partial |

---

## Priority Components (Deep Dive)

### TransactionInsightService

| Field | Value |
|-------|-------|
| **File** | `src/main/java/com/flowiq/service/TransactionInsightService.java` |
| **Type** | Service |
| **Called by** | None (`grep TransactionInsightService` → only this class + old docs) |
| **Production** | `@Service` bean, never called |
| **Real calls** | No |
| **Future hook** | Yes — `buildAnalysisContext(User, from, to)` |

**Code reference:** Javadoc states “Preparation layer for future AI-powered transaction analysis.” Method aggregates revenue/expenses and maps transactions to `TransactionSnapshot` list.

---

### AnalyticsInsightProvider

| Field | Value |
|-------|-------|
| **File** | `src/main/java/com/flowiq/analytics/AnalyticsInsightProvider.java` |
| **Type** | Provider interface |
| **Called by** | `AnalyticsService` constructor stores `insightProviders`; **no method reads the field** |
| **Production** | No implementations (`implements AnalyticsInsightProvider` → 0 matches) |
| **Real calls** | No |
| **Future hook** | Yes — intended `getInsights(Long userId)` |

**Code reference:** `AnalyticsService.java` lines 49–60 (injection only).

---

### AIInsightProvider

| Field | Value |
|-------|-------|
| **File** | `src/main/java/com/flowiq/aiaccountant/AIInsightProvider.java` |
| **Type** | Provider interface |
| **Called by** | `AIAccountantService.getRecommendations()` (merge loop), `chat()` (try providers first) |
| **Production** | No implementations |
| **Real calls** | Wiring executes empty `for` loops |
| **Future hook** | Yes — partially integrated |

**Code reference:** `AIAccountantService.java` lines 107–109, 145–149.

---

### CategorizationProvider

| Field | Value |
|-------|-------|
| **File** | `src/main/java/com/flowiq/categorization/CategorizationProvider.java` |
| **Type** | Provider interface |
| **Called by** | `CategorizationEngine.categorize()` after `DefaultCategoryRules` |
| **Production** | No implementations |
| **Real calls** | Empty provider loop; rules + fallback `"Other"` |
| **Future hook** | Yes |

**Code reference:** `CategorizationEngine.java` lines 46–51.

---

### DashboardService

| Field | Value |
|-------|-------|
| **File** | `src/main/java/com/flowiq/service/DashboardService.java` |
| **Type** | Service (inline intelligence) |
| **Called by** | `DashboardController` |
| **Production** | Yes |
| **Real calls** | `getInsights()`, `getBusinessHealth()`, `getAISummary()`, stats/charts |
| **Future hook** | No provider injection |

**Frontend:** `flowiq-frontend/src/services/dashboard.service.ts`

**Insights rules (actual):** month-over-month expense vs revenue growth thresholds, positive profit, default fallback — **not** category concentration or `TransactionInsightService`.

**Code reference:** `getInsights()` lines 62–158.

---

### ForecastEngine

| Field | Value |
|-------|-------|
| **File** | `src/main/java/com/flowiq/forecasts/engine/ForecastEngine.java` |
| **Type** | Engine (`@Component`) |
| **Called by** | `ForecastService` exclusively |
| **Production** | Yes |
| **Real calls** | `projectMonths`, `analyzeTrend`, `rollingAverage`, `sumHorizon` |
| **Future hook** | No |

**Not used by:** `AIAccountantService.getForecasts()` (inline `buildForecast()` at lines 383–403).

**Frontend:** `flowiq-frontend/src/features/forecasts/services/forecast.service.ts`, dashboard `forecast-snapshot` via `dashboard.service.ts`.

---

### AIRecommendationEngine

| Field | Value |
|-------|-------|
| **File** | `src/main/java/com/flowiq/aiaccountant/AIRecommendationEngine.java` |
| **Type** | Engine (`@Component`) |
| **Called by** | `AIAccountantService.getRecommendations()` |
| **Production** | Yes |
| **Real calls** | `generate(FinancialSnapshot)` |
| **Future hook** | No (baseline; providers augment) |

**Frontend:** `flowiq-frontend/src/features/ai-accountant/services/aiAccountantService.ts` → `GET /ai-accountant/recommendations`

**Code reference:** `AIAccountantService.java` line 105.

---

## Frontend AI Surface (No Server Logic)

| UI module | Service file | Backend endpoints |
|-----------|--------------|-------------------|
| Dashboard insights/health/summary | `src/services/dashboard.service.ts` | `/api/dashboard/insights`, `/health`, `/summary` |
| AI Accountant | `src/features/ai-accountant/services/aiAccountantService.ts` | `/api/ai-accountant/*` |
| Forecast Center | `src/features/forecasts/services/forecast.service.ts` | `/api/forecasts/*` |
| Analytics | `src/features/analytics/services/analyticsService.ts` | `/api/analytics/*` |
| Chat (standalone) | `src/services/chat.service.ts` | `/api/chat/message` |
| Business Guide search | `src/features/business-guide/services/knowledge.service.ts` | `/api/business-guide/search` |

**Mocks still used (non-AI):** `tax-profile.service.ts` → `mock-data/tax-profile.localized`; Business Guide checker static data.

---

## Documentation ↔ Code Discrepancies Found

| # | Doc claim (before) | Code truth | Severity |
|---|-------------------|------------|----------|
| 1 | `DashboardService.getInsights()` uses `TransactionInsightService` | Inline rules only; TIS has zero callers | **High** |
| 2 | Dashboard insights: “revenue spikes, category concentration, cash flow warnings” | Only MoM growth + profit sign rules | **High** |
| 3 | `DefaultCategoryRules` listed as `CategorizationProvider` default | Rules embedded in `CategorizationEngine`, not a provider | **Medium** |
| 4 | Forecast: “all providers merged” everywhere | Extra providers append **summary insights only**; warnings rule-only | **Medium** |
| 5 | `ForecastEngine` aggregates raw transactions | `ForecastService.buildHistoricalData()` aggregates; engine is pure math | **Medium** |
| 6 | AI Accountant `/forecasts` tied to `ForecastEngine` | `AIAccountantService.buildForecast()` inline | **Medium** |
| 7 | `AnalyticsInsightProvider` “consumer: AnalyticsService” implies use | Injected, never called | **High** |
| 8 | `ai-architecture.md` sequence: `buildRecommendations(context)` | Actual: `generate(snapshot)` | **Low** |
| 9 | `TransactionInsightService` output includes “category breakdowns” | Per-transaction category in snapshots only | **Low** |
| 10 | Architecture diagram: TX → TIS active path | TIS disconnected (future hook) | **Low** |

---

## Documentation Files Updated

| File | Changes |
|------|---------|
| `docs/ai/insights-engine.md` | Removed false TIS link; added component tables with file/caller/production flags; documented actual dashboard rules |
| `docs/ai/providers.md` | Fixed categorization default; documented per-interface invocation state; added code line references |
| `docs/ai/forecast-engine.md` | Clarified ForecastService vs ForecastEngine responsibilities; noted AI Accountant forecast separation |
| `docs/ai/future-llm-integration.md` | Added wired/invoked columns; documented TIS as uncalled prep layer |
| `docs/ai/knowledge-search.md` | Added controller/service/provider file paths |
| `docs/architecture/ai-architecture.md` | Component registry; corrected provider table, forecast merge rules, sequence diagram method names |
| `docs/architecture/ai-agents-architecture.md` | Updated diagram (TIS orphan); expanded priority unit metadata; fixed invocation matrix |
| `docs/architecture/ai-quality-factory.md` | Corrected forecast/analytics provider selection; noted AI Accountant forecast path |
| `docs/architecture/ARCHITECTURE_REVIEW_READINESS.md` | Linked this report; updated intelligence findings and doc health score |

---

## Remaining Code Gaps (Not Documentation)

These are **code** issues surfaced during the audit — not fixed in this pass:

1. **Dead code:** `TransactionInsightService` — consider wiring into dashboard insights or removing until LLM ships.
2. **Dead injection:** `AnalyticsService.insightProviders` — invoke providers or remove field.
3. **Duplicate forecast logic:** `AIAccountantService.buildForecast()` vs `ForecastService`/`ForecastEngine`.
4. **Duplicate health scoring:** `DashboardService.calculateHealthScore()` vs `AIAccountantService.calculateHealthScore()` (different formulas).
5. **No LLM SDKs** in `pom.xml` / dependencies — confirms rule-only production.

---

## Verification Commands Used

```bash
# No implementors for three provider interfaces
rg "implements (AIInsightProvider|AnalyticsInsightProvider|CategorizationProvider)" src/main/java

# TransactionInsightService callers
rg "TransactionInsightService" 

# AnalyticsInsightProvider usage
rg "insightProviders" src/main/java/com/flowiq/service/AnalyticsService.java
```

---

## Related Documents

- [AI Architecture](ai-architecture.md)
- [AI Agents Architecture](ai-agents-architecture.md)
- [AI Quality Factory](ai-quality-factory.md)
- [Providers](../ai/providers.md)
- [Insights Engine](../ai/insights-engine.md)

**Prepared by:** AI documentation audit (2026-06-23)  
**Next review:** When first `AIInsightProvider` / LLM implementation is added
