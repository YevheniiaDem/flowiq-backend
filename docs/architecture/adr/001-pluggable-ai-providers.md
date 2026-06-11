# ADR-001: Pluggable AI Providers

**Status:** Accepted  
**Date:** 2026-06-11  
**Context:** FlowIQ needs AI-style insights without locking into a single LLM vendor.

## Decision

Use **interface-based provider pattern** with Spring `@Component` auto-discovery:

- `ForecastProvider`
- `KnowledgeProvider`
- `AIInsightProvider`
- `AnalyticsInsightProvider`
- `CategorizationProvider`

Ship **rule-based default implementations** that work without external API keys.

## Consequences

### Positive
- Demo and development work offline
- Predictable, testable outputs for QA
- Swap or combine LLM vendors without controller changes
- Ukrainian FOP domain logic stays in code, not prompts

### Negative
- Two code paths to maintain (rules + LLM)
- Provider priority logic must be documented (`KnowledgeService` skips `DatabaseKnowledgeProvider` when other beans exist)

## Implementation Notes

```java
// Example: add OpenAI forecast provider
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class OpenAiForecastProvider implements ForecastProvider {
    @Override
    public List<ForecastInsightDto> generateInsights(ForecastContext context) { ... }
}
```

## Alternatives Considered

1. **Direct OpenAI calls in services** — rejected (tight coupling)
2. **Single AI gateway microservice** — deferred (monolith sufficient for MVP)
3. **Prompt-only, no rules** — rejected (compliance risk for tax advice)

## Related

- [AI Architecture](../ai-architecture.md)
- [Future LLM Integration](../../ai/future-llm-integration.md)
