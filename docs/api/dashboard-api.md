# Dashboard API

**Controller:** `com.flowiq.controller.DashboardController`  
**Base path:** `/api/dashboard`  
**Auth:** JWT required

## Endpoints

| Method | Path | Response Type | Purpose |
|--------|------|---------------|---------|
| GET | `/stats` | `List<StatCardResponse>` | Revenue, expenses, profit, cash flow cards |
| GET | `/insights` | `List<AIInsightResponse>` | Dashboard insight cards |
| GET | `/health` | `BusinessHealthResponse` | Overall health score |
| GET | `/summary` | `AISummaryResponse` | Narrative AI summary |
| GET | `/charts/revenue-trend` | `List<MonthlyAmountResponse>` | 6-month revenue chart |
| GET | `/charts/expense-breakdown` | `List<CategoryAmountResponse>` | Expense by category |
| GET | `/forecast-snapshot` | `ForecastSnapshotResponse` | Widget: 3-month projection |
| GET | `/tasks-snapshot` | `TaskSnapshotResponse` | Widget: today + overdue |
| GET | `/business-guide-snapshot` | `KnowledgeDashboardSnapshotDto` | Widget: popular/recent articles |

## Example: Stats

```http
GET /api/dashboard/stats
Authorization: Bearer <token>
```

```json
[
  { "labelKey": "revenue", "amount": 485000, "change": 12.5, "changeType": "positive", "icon": "trending-up" },
  { "labelKey": "expenses", "amount": 120000, "change": -3.2, "changeType": "negative", "icon": "wallet" }
]
```

## Example: Forecast Snapshot

Delegates to `ForecastService.getSnapshot()` — compact revenue/profit/tax projection for dashboard widget.

## Example: Tasks Snapshot

```json
{
  "todayCount": 3,
  "overdueCount": 1,
  "todayTasks": [...],
  "upcomingDeadlines": [...]
}
```

## Frontend

`DashboardView` loads all endpoints in parallel via `dashboardService` + `knowledgeService.getDashboardSnapshot()` (Business Guide widget).

## Related

- [Dashboard Module](../modules/dashboard.md)
- [Forecast API](forecast-api.md)
- [Tasks API](tasks-api.md)
