# AI Providers

Pluggable interfaces for AI capabilities. See [ADR-001](../architecture/adr/001-pluggable-ai-providers.md).

| Interface | Package | Default | Inject Into |
|-----------|---------|---------|-------------|
| `ForecastProvider` | `forecasts.provider` | `RuleBasedForecastProvider` | `ForecastService` |
| `KnowledgeProvider` | `knowledge.provider` | `DatabaseKnowledgeProvider` | `KnowledgeService` |
| `AIInsightProvider` | `aiaccountant` | — | `AIAccountantService` |
| `AnalyticsInsightProvider` | `analytics` | — | `AnalyticsService` |
| `CategorizationProvider` | `categorization` | `DefaultCategoryRules` | `CategorizationEngine` |

## Registration

Implement interface + `@Component`. Spring auto-wires `List<Provider>`.

## Priority

- **Knowledge:** First non-`DatabaseKnowledgeProvider` wins
- **Forecast:** All providers merged
- **Categorization:** Rules first, then AI providers

## Related

- [Future LLM Integration](future-llm-integration.md)
