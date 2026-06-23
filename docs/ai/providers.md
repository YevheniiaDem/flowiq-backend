# AI Providers

Pluggable interfaces for AI capabilities. See [ADR-001](../architecture/adr/001-pluggable-ai-providers.md).

**Production today:** rule-based defaults only. No class in `src/main/java` implements `AIInsightProvider`, `AnalyticsInsightProvider`, or `CategorizationProvider`.

## Interface Registry

| Interface | Package / file | Default behavior | Injected into | Invoked in production |
|-----------|----------------|------------------|---------------|----------------------|
| `ForecastProvider` | `com.flowiq.forecasts.provider` | `RuleBasedForecastProvider` | `ForecastService` | Yes — insights on `/api/forecasts/summary` |
| `KnowledgeProvider` | `com.flowiq.knowledge.provider` | `DatabaseKnowledgeProvider` | `KnowledgeService` | Yes — `assistSearch()` on search |
| `AIInsightProvider` | `com.flowiq.aiaccountant.AIInsightProvider` | `AIRecommendationEngine` (not a provider; separate bean) | `AIAccountantService` | Wired in `getRecommendations()` / `chat()`; **no provider beans** |
| `AnalyticsInsightProvider` | `com.flowiq.analytics.AnalyticsInsightProvider` | Inline rules in `AnalyticsService` | `AnalyticsService` | **Injected only — field never used** |
| `CategorizationProvider` | `com.flowiq.categorization.CategorizationProvider` | `DefaultCategoryRules` inside `CategorizationEngine` (not a provider) | `CategorizationEngine` | Wired in `categorize()`; **no provider beans** |

> `DefaultCategoryRules` is a static rule list used by `CategorizationEngine` — it does **not** implement `CategorizationProvider`.

## Registration

Implement interface + `@Component`. Spring injects `List<Provider>` with `@Autowired(required = false)`.

## Selection Logic (as implemented)

### Forecast — `ForecastService.getSummary()`

```java
// RuleBasedForecastProvider always used for insights baseline + all warnings
insights.addAll(ruleBasedProvider.generateInsights(context));
for (ForecastProvider provider : forecastProviders) {
    if (!(provider instanceof RuleBasedForecastProvider)) {
        insights.addAll(provider.generateInsights(context));
    }
}
warnings = ruleBasedProvider.generateWarnings(context); // no provider merge
```

Source: `src/main/java/com/flowiq/forecasts/service/ForecastService.java` (lines ~200–209).

### Knowledge — `KnowledgeService.resolveAssistResult()`

First non-`DatabaseKnowledgeProvider` bean wins; else `DatabaseKnowledgeProvider.assistSearch()`.

Source: `src/main/java/com/flowiq/knowledge/service/KnowledgeService.java` (lines ~201–208).

### Categorization — `CategorizationEngine.categorize()`

1. `DefaultCategoryRules` keyword match
2. First `CategorizationProvider` returning `isAutoCategorized() == true`
3. Fallback category `"Other"`

Source: `src/main/java/com/flowiq/categorization/CategorizationEngine.java` (lines ~37–54).

### AI Accountant — `AIAccountantService`

1. `AIRecommendationEngine.generate(snapshot)` always runs for recommendations
2. Then merge each `AIInsightProvider.getRecommendations(snapshot)`
3. Chat: try `AIInsightProvider.answerChat()` first; else `generateChatReply()` templates

Source: `src/main/java/com/flowiq/service/AIAccountantService.java` (lines ~103–155).

### Analytics — `AnalyticsService`

`AnalyticsInsightProvider` list is assigned in the constructor and **never referenced** elsewhere in the class.

Source: `src/main/java/com/flowiq/service/AnalyticsService.java` (lines ~49–60, no usages of `insightProviders`).

## Related

- [Future LLM Integration](future-llm-integration.md)
- [AI Documentation Audit Report](../architecture/AI_DOCUMENTATION_AUDIT_REPORT.md)
