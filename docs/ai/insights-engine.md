# Insights Engine

Rule-based insight generation across modules.

## Dashboard Insights

**Service:** `DashboardService.getInsights()`  
Uses `TransactionInsightService` to analyze patterns:
- Revenue spikes/drops
- Category concentration
- Cash flow warnings

## AI Accountant

**Engine:** `AIRecommendationEngine`  
**Service:** `AIAccountantService`

Generates:
- Tax optimization tips
- FOP group recommendations
- Risk warnings

Endpoints: `/api/ai-accountant/recommendations`, `/tax-advisor`, `/forecasts`, `POST /chat`

## Forecast Insights

**Provider:** `RuleBasedForecastProvider.generateInsights()`

Examples:
- "Income may exceed FOP limit in 4 months"
- "Expense growth outpacing revenue"

## Notification Insights

**Engine:** `NotificationRuleEngine`  
Converts thresholds into user-visible alerts with `action_url`.

## Extension

Replace or augment with `AIInsightProvider` beans for LLM-generated narratives.

## Related

- [AI Architecture](../architecture/ai-architecture.md)
