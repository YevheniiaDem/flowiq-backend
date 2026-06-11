# Analytics Module

**Controller:** `AnalyticsController`  
**Service:** `AnalyticsService`  
**Frontend:** `features/analytics/`

## Endpoints

| Path | Purpose |
|------|---------|
| `/analytics/overview` | Summary stats |
| `/analytics/revenue-trend` | 12-month revenue |
| `/analytics/expense-breakdown` | By category |
| `/analytics/profit-trend` | Profit over time |
| `/analytics/income-vs-expenses` | Comparison chart |
| `/analytics/fop-insights` | FOP group, limit %, tax load |

## FOP Insights

`FopInsightsResponse` includes current group, income limit usage, estimated tax load, days until next tax payment.

## Extension

`AnalyticsInsightProvider` for future LLM analytics narratives.

## Frontend

`AnalyticsView` with Recharts visualizations.
