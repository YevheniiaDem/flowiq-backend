# Unit Test Coverage Report

**Project:** `flowiq-backend`  
**Package:** `com.flowiq.unit`  
**Stack:** JUnit 5, Mockito, AssertJ  
**Goal:** ≥ 70% business-logic coverage  
**Last run:** 2026-06-12

## Summary

| Metric | Value |
|--------|-------|
| Test classes | 7 |
| Test methods | 88 |
| SpringBootTest usage | None (pure unit tests) |
| JaCoCo report | `target/site/jacoco/index.html` |

All priority service/engine classes meet the **70% instruction coverage** target.

## Coverage by Class

| Class | Instruction | Line | Branch | Methods covered |
|-------|-------------|------|--------|-----------------|
| `ForecastEngine` | **99.4%** | **100%** | 90.0% | 10/10 |
| `ForecastService` | **92.4%** | **94.4%** | 54.2% | 30/30 |
| `AIRecommendationEngine` | **82.0%** | **84.7%** | 69.2% | 6/6 |
| `NotificationRuleEngine` | **84.0%** | **84.4%** | 71.7% | 15/17 |
| `TaskRuleEngine` | **91.7%** | **95.0%** | 73.8% | 14/14 |
| `KnowledgeService` | **88.3%** | **93.7%** | 60.5% | 20/24 |
| `ReportsService` | **85.2%** | **87.9%** | 64.3% | 17/24 |

> Percentages from JaCoCo (`target/site/jacoco/jacoco.csv`) after `mvn test -Dtest=com.flowiq.unit.**`.

## Test Structure

```
src/test/java/com/flowiq/unit/
├── aiaccountant/
│   └── AIRecommendationEngineTest.java      (11 tests)
├── forecasts/
│   ├── ForecastEngineTest.java              (20 tests)
│   └── ForecastServiceTest.java             (12 tests)
├── knowledge/
│   └── KnowledgeServiceTest.java            (12 tests)
├── notifications/
│   └── NotificationRuleEngineTest.java      (10 tests)
├── reports/
│   └── ReportsServiceTest.java              (13 tests)
├── tasks/
│   └── TaskRuleEngineTest.java              (10 tests)
└── support/
    └── SecurityTestSupport.java             (auth helper)
```

## Scenarios Covered

### ForecastEngine (pure logic)
- Happy path: trend analysis, rolling average, projection, horizon sum
- Edge cases: null/empty input, single value, window larger than data
- Boundaries: zero months ahead, limit already reached, invalid limit/monthly revenue

### ForecastService (mocked dependencies)
- All public forecast endpoints: revenue, expense, profit, tax, FOP limit, summary, snapshot
- Zero transactions, high YTD income (FOP group resolution), Ukrainian locale
- Auth failures: unauthenticated, user not found

### AIRecommendationEngine (pure logic)
- All recommendation rules: FOP critical, expense spike, infrastructure, profit growth, tax, revenue growth
- Stable fallback, Ukrainian/English locales, multiple simultaneous signals, zero revenue

### NotificationRuleEngine
- FOP thresholds: 70%, 85%, 95%
- Expense spike, revenue drop, profit growth (3 months)
- Tax deadline reminders (fixed date via `MockedStatic<LocalDate>`)
- Skips: income above limits, zero previous expenses

### TaskRuleEngine
- FOP review tasks with priority by usage
- Tax payment, ESV, quarter/annual declaration (date-mocked)
- Expense growth vs revenue, monthly report (day-of-month boundary)
- Empty data handling without exceptions

### KnowledgeService
- Pagination, category/tag filters, slug lookup, view count increment
- Search (blank query, with query, category-scoped), empty results assist message
- Dashboard snapshot, EN/UK category labels
- `ResourceNotFoundException` for missing slug

### ReportsService
- List/get/preview/generate/download flows
- Period presets: `THIS_MONTH`, `YEAR`, custom range, invalid `dateFrom > dateTo`
- Generation failure → `FAILED` status, completed → notification + task
- Auth and not-found errors

## How to Run

From `flowiq-backend`:

```bash
# Unit tests only
mvn test -Dtest=com.flowiq.unit.**

# With coverage report
mvn test -Dtest=com.flowiq.unit.**
# Open target/site/jacoco/index.html
```

## Design Decisions

1. **Location:** Tests live in `flowiq-backend` (not `flowiq-automation`) because business classes are compiled there.
2. **No `@SpringBootTest`:** Services are instantiated with `@InjectMocks` / constructor injection; repositories and generators are mocked.
3. **Real engines where possible:** `ForecastEngine` and `RuleBasedForecastProvider` are used as real instances in `ForecastServiceTest` to exercise projection logic without Spring context.
4. **Deterministic dates:** `NotificationRuleEngine` and `TaskRuleEngine` tax-calendar tests use `MockedStatic<LocalDate>` for stable deadline scenarios.
5. **JaCoCo:** Configured in `pom.xml` (`jacoco-maven-plugin` 0.8.12).

## Gaps / Follow-ups

| Area | Notes |
|------|-------|
| `KnowledgeService` | Optional `KnowledgeProvider` list not injected in tests (DB provider only) |
| `ForecastService` | Optional `ForecastProvider` plugins not tested |
| `ReportsService` | Not all `ReportType` switch branches asserted individually |
| `NotificationRuleEngine` | Tax optimization notification (group savings > 1000) not isolated |

These are lower-risk paths; core business rules for the seven priority classes are covered above the 70% threshold.
