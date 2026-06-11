# Forecast API

**Controller:** `com.flowiq.forecasts.controller.ForecastController`  
**Base path:** `/api/forecasts`  
**Auth:** JWT required

## Endpoints

| Method | Path | Response | Description |
|--------|------|----------|-------------|
| GET | `/revenue` | `ForecastMetricResponse` | Revenue forecast with trend |
| GET | `/expenses` | `ForecastMetricResponse` | Expense forecast |
| GET | `/profit` | `ForecastMetricResponse` | Profit forecast |
| GET | `/taxes` | `TaxForecastResponse` | Tax burden projection |
| GET | `/fop-limit` | `FopLimitForecastResponse` | Annual FOP limit usage |
| GET | `/summary` | `ForecastSummaryResponse` | Full summary + insights + warnings |

## Example: Revenue Forecast

```http
GET /api/forecasts/revenue
Authorization: Bearer <token>
```

```json
{
  "metric": "revenue",
  "historical": [{ "month": "2026-01", "amount": 380000 }],
  "projected": [{ "month": "2026-07", "amount": 420000 }],
  "trendPercent": 8.5,
  "horizonMonths": 6
}
```

## Example: FOP Limit

```json
{
  "currentAnnualIncome": 4850000,
  "incomeLimit": 5328000,
  "usagePercent": 91.0,
  "monthsUntilLimit": 4,
  "fopGroup": 2
}
```

## Data Source

All forecasts computed from authenticated user's `transactions` via `TransactionRepository` — no separate forecast storage.

## Engine Pipeline

See [Forecast Center Module](../modules/forecast-center.md) and [Forecast Engine](../ai/forecast-engine.md).

## Related

- [Dashboard forecast snapshot](dashboard-api.md)
