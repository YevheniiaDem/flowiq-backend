# Forecast Engine

| Attribute | Value |
|-----------|-------|
| **Class** | `com.flowiq.forecasts.engine.ForecastEngine` |
| **Type** | Engine (`@Component`) |
| **Called by** | `ForecastService` only |
| **Production** | Yes |
| **Future hook** | No |

## Responsibilities

`ForecastEngine` performs **pure math** on pre-aggregated monthly series. It does **not** read the database or group raw transactions — that is `ForecastService.loadForecastData()` → `buildHistoricalData()`.

1. Rolling averages (`rollingAverage`, window = `ROLLING_WINDOW` = 3)
2. Trend analysis (`analyzeTrend`, window = `TREND_WINDOW` = 6)
3. Forward projection (`projectMonths`, horizon = `PROJECTION_MONTHS` = 12)
4. Horizon sums (`sumHorizon`, horizons = `{1, 3, 6, 12}`)

Constants: `ForecastEngine.FORECAST_HORIZONS`, `ROLLING_WINDOW`, `TREND_WINDOW`, `PROJECTION_MONTHS`.

## Key Types

| Type | File | Purpose |
|------|------|---------|
| `MonthlyFinancialData` | `forecasts/engine/MonthlyFinancialData.java` | Single month revenue/expense/profit |
| `TrendAnalysis` | `forecasts/engine/TrendAnalysis.java` | Percent change between recent and older halves of trend window |

## Data Flow

```
TransactionRepository
    → ForecastService.buildHistoricalData()   // 12 monthly buckets
    → ForecastEngine.projectMonths()          // 12 projected months
    → ForecastEngine.analyzeTrend()           // revenue/expense trends
    → ForecastService DTO builders
    → RuleBasedForecastProvider (narratives on /summary only)
```

## Algorithm (simplified)

```
historical = ForecastService.buildHistoricalData(userId)   // not inside ForecastEngine
baseRevenue = rollingAverage(revenueHistory, window=3)
baseExpenses = rollingAverage(expenseHistory, window=3)
trend = analyzeTrend(history)   // recent half vs older half of 6-month window
projected[m] = applyGrowth(base, trend.monthlyGrowthRate) per month
```

## Outputs (via ForecastService)

| Endpoint | Uses ForecastEngine |
|----------|---------------------|
| `GET /api/forecasts/revenue` | Yes |
| `GET /api/forecasts/expenses` | Yes |
| `GET /api/forecasts/profit` | Yes |
| `GET /api/forecasts/taxes` | Yes |
| `GET /api/forecasts/fop-limit` | Yes |
| `GET /api/forecasts/summary` | Yes + `RuleBasedForecastProvider` |
| `GET /api/dashboard/forecast-snapshot` | Yes (`getSnapshot()`) |

**Not using ForecastEngine:** `AIAccountantService.getForecasts()` — separate inline projection in `buildForecast()`.

## Frontend

`flowiq-frontend/src/features/forecasts/services/forecast.service.ts` calls `/api/forecasts/*`.  
Dashboard widget: `dashboard.service.ts` → `/api/dashboard/forecast-snapshot`.

## Related

- [Forecast Center](../modules/forecast-center.md)
- [Providers](providers.md)
