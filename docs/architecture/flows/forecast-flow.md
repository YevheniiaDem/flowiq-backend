# Forecast Flow

**As-built:** 2026-06-28  
**Backend:** `ForecastController`, `ForecastService`, `ForecastEngine`, `RuleBasedForecastProvider`  
**Frontend:** `features/forecasts`

## Overview

Forecast Center projects revenue, expenses, profit, taxes, and FOP income limits using **historical transaction data** and **trend-based math**. Narrative insights and warnings come from `RuleBasedForecastProvider`; optional `ForecastProvider` beans can extend insights (none in production).

## Request Flow

```mermaid
sequenceDiagram
    participant UI as ForecastCenterView
    participant FC as ForecastController
    participant FS as ForecastService
    participant TSS as TransactionSeedService
    participant TR as TransactionRepository
    participant FPS as FopProfileService
    participant FE as ForecastEngine
    participant RFP as RuleBasedForecastProvider
    participant DB as PostgreSQL

    UI->>FC: GET /api/forecasts/summary (or /revenue, /expenses, ...)
    FC->>FS: getSummary() / getRevenueForecast() / ...
    FS->>TSS: seedIfEmpty(user) if no transactions
    FS->>TR: load 12 months history
    TR->>DB: SELECT transactions by user + date
    FS->>FPS: get FOP group for tax/limit calcs
    FS->>FE: buildMonthlyTotals + projectTrend
    FE-->>FS: historical + projected MonthlyFinancialData
    FS->>RFP: generateInsights + generateWarnings
    opt Additional ForecastProvider beans
        FS->>FS: merge provider insights
    end
    FS-->>UI: ForecastSummaryResponse / ForecastMetricResponse
```

## Engine Pipeline

```mermaid
flowchart TB
    subgraph Input
        TXN[(transactions 12mo)]
        FOP[FOP profile group]
    end

    subgraph ForecastEngine
        AGG[Aggregate monthly revenue/expense]
        TREND[TrendAnalysis 6mo window]
        PROJ[Project 1/3/6/12 month horizons]
    end

    subgraph Outputs
        METRICS[Metric endpoints]
        TAX[Tax forecast cards]
        FOP_LIM[FOP limit projection]
        INSIGHTS[RuleBasedForecastProvider insights]
        WARN[Warnings FOP/tax/trend]
    end

    TXN --> AGG --> TREND --> PROJ
    FOP --> TAX & FOP_LIM
    PROJ --> METRICS & TAX & FOP_LIM
    PROJ --> INSIGHTS & WARN
```

## Forecast Horizons

| Constant | Value | Usage |
|----------|-------|---------|
| `FORECAST_HORIZONS` | 1, 3, 6, 12 months | All metric cards |
| History window | 12 months | Data load |
| Trend window | 6 months | Growth calculation |
| Rolling average | 3 months | Smoothing |

**Source:** `ForecastEngine.java`

## API Endpoints

| Endpoint | Returns |
|----------|---------|
| `GET /api/forecasts/revenue` | Historical + projected revenue series |
| `GET /api/forecasts/expenses` | Expense series |
| `GET /api/forecasts/profit` | Profit series |
| `GET /api/forecasts/taxes` | Tax forecast cards (single tax + ЄСВ estimate) |
| `GET /api/forecasts/fop-limit` | YTD vs annual limit, projected breach |
| `GET /api/forecasts/summary` | Combined dashboard + insights + warnings |

## Dashboard Integration

```mermaid
flowchart LR
    DASH[DashboardController] --> FS[ForecastService.getSnapshot]
    FS --> WIDGET[Forecast snapshot widget]
```

## Notification Integration

`NotificationRuleEngine` (daily 08:00) may create `AI_FORECAST_ANOMALY` notifications when forecast warnings match preference-enabled rules.

## Separate: AI Accountant Forecasts

`GET /api/ai-accountant/forecasts` uses **different logic** in `AIAccountantService` (simplified 3/6/12-month projection). Not identical to Forecast Center output.

```mermaid
flowchart LR
    FC_API["/api/forecasts/*"] --> FS[ForecastService + ForecastEngine]
    AI_API["/api/ai-accountant/forecasts"] --> AIS[AIAccountantService]
    FS -.->|different algorithms| AIS
```

## Extension Point (Future LLM)

```mermaid
flowchart LR
    FS[ForecastService.getSummary] --> RBF[RuleBasedForecastProvider always]
    FS --> OPT["ForecastProvider beans (optional)"]
    OPT -.-> LLM[Future LLM provider]
    RBF --> INS[Baseline insights + all warnings]
    OPT -.-> INS2[Additional insights only]
```

## Related

- [ai-architecture.md](../ai-architecture.md)
- [Forecast Engine](../../ai/forecast-engine.md)
- [Forecast API](../../api/forecast-api.md)
- [flows/ai-flow.md](ai-flow.md)
