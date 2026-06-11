# Forecast Engine

**Class:** `com.flowiq.forecasts.engine.ForecastEngine`

## Responsibilities

1. Aggregate transactions into monthly buckets
2. Compute rolling averages (3-month default)
3. Project future months with trend adjustment
4. Supply context to `ForecastProvider` implementations

## Key Types

| Type | Purpose |
|------|---------|
| `MonthlyFinancialData` | Single month revenue/expense/profit |
| `TrendAnalysis` | Percent change calculations |

## Algorithm (Simplified)

```
historical = groupByMonth(transactions, last 12 months)
baseline = rollingAverage(historical, window=3)
trend = percentChange(recent 3 vs prior 3)
projected[m] = baseline * (1 + trend * m_factor)
```

## Outputs

Consumed by `ForecastService` for:
- `ForecastMetricResponse` (revenue/expense/profit)
- `TaxForecastResponse`
- `FopLimitForecastResponse`
- `ForecastSummaryResponse` (+ provider insights)

## Related

- [Forecast Center](../modules/forecast-center.md)
