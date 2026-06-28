# AI & Intelligence Flow

**As-built:** 2026-06-28  
**Scope:** All rule-based "AI" capabilities and provider extension points

> Provider interface detail: [ai-architecture.md](../ai-architecture.md)

## Overview

Production AI features are **deterministic rule engines** inside the backend JVM. Five **provider interfaces** allow future LLM beans; none are wired today.

## Intelligence Architecture

```mermaid
flowchart TB
    subgraph Controllers
        DASH_C[DashboardController]
        AI_C[AIAccountantController]
        FC_C[ForecastController]
        BG_C[BusinessGuideController]
        CH_C[ChatController]
        IMP[ImportService]
    end

    subgraph Services
        DASH_S[DashboardService]
        AI_S[AIAccountantService]
        FC_S[ForecastService]
        KS[KnowledgeService]
        CH_S[ChatService]
    end

    subgraph Engines["Rule Engines (production)"]
        ARE[AIRecommendationEngine]
        FE[ForecastEngine]
        RFP[RuleBasedForecastProvider]
        CE[CategorizationEngine]
        DKP[DatabaseKnowledgeProvider]
    end

    subgraph Optional["Optional Providers (future)"]
        AIP[AIInsightProvider*]
        FP[ForecastProvider*]
        KP[KnowledgeProvider*]
        CP[CategorizationProvider*]
        AAP[AnalyticsInsightProvider*]
    end

    DASH_C --> DASH_S --> ARE
    AI_C --> AI_S --> ARE & AIP
    FC_C --> FC_S --> FE & RFP & FP
    BG_C --> KS --> DKP & KP
    CH_C --> CH_S
    IMP --> CE & CP

    AIP -.-> LLM[External LLM]
    FP -.-> LLM
    KP -.-> LLM
    CP -.-> LLM
    AAP -.-> LLM
```

## AI Accountant Recommendations Flow

```mermaid
sequenceDiagram
    participant UI as AIAccountantView
    participant AC as AIAccountantController
    participant AIS as AIAccountantService
    participant TR as TransactionRepository
    participant AN as AnalyticsService
    participant ARE as AIRecommendationEngine
    participant AIP as AIInsightProvider (optional)

    UI->>AC: GET /api/ai-accountant/recommendations
    AC->>AIS: getRecommendations()
    AIS->>TR: build FinancialSnapshot
    AIS->>AN: getFopInsights()
    AIS->>ARE: generate(snapshot)
    ARE-->>AIS: Rule-based recommendations
    opt AIInsightProvider beans present
        AIS->>AIP: getRecommendations(snapshot)
        AIS->>AIS: merge lists
    end
    AIS-->>UI: AIRecommendationResponse[]
```

## AI Accountant Chat Flow

```mermaid
sequenceDiagram
    participant UI as AIAccountantView
    participant AC as AIAccountantController
    participant AIS as AIAccountantService
    participant AIP as AIInsightProvider (optional)

    UI->>AC: POST /api/ai-accountant/chat
    AC->>AIS: chat(request)
    opt AIInsightProvider.answerChat
        AIS->>AIP: answerChat(message, snapshot)
        AIP-->>AIS: LLM reply (future)
    end
    alt No provider reply
        AIS->>AIS: generateChatReply() keyword templates
    end
    AIS-->>UI: AIAccountantChatResponse
```

## General Chat Flow (Separate Module)

```mermaid
sequenceDiagram
    participant UI as ChatView
    participant CC as ChatController
    participant CS as ChatService
    participant CR as ChatConversationRepository
    participant MR as ChatMessageRepository

    UI->>CC: POST /api/chat/message
    CC->>CS: sendMessage(request)
    CS->>CR: findOrCreate conversation
    CS->>MR: save user message
    CS->>CS: generateReply() keyword rules
    CS->>MR: save assistant message
    CS-->>UI: conversation + messages
```

**Note:** Two chat systems — different persistence and APIs.

## Dashboard AI Flow

```mermaid
sequenceDiagram
    participant UI as DashboardView
    participant DC as DashboardController
    participant DS as DashboardService
    participant TR as TransactionRepository

    par Parallel widget loads
        UI->>DC: GET /stats
        UI->>DC: GET /insights
        UI->>DC: GET /summary
        UI->>DC: GET /health
    end
    DC->>DS: getInsights / getAISummary / getHealthScore
    DS->>TR: aggregate transactions
    DS->>DS: threshold rules + template narratives
    DS-->>UI: AIInsightResponse / AISummaryResponse
```

## Categorization Flow (Import)

```mermaid
flowchart LR
    DESC[Transaction description] --> DCR[DefaultCategoryRules]
    DCR --> CP{CategorizationProvider?}
    CP -->|None| OTHER[Other category]
    CP -->|Future| ML[ML/LLM category]
    DCR --> OUT[CategorizationResult]
```

## Knowledge Search Assist Flow

```mermaid
sequenceDiagram
    participant UI as BusinessGuideView
    participant BC as BusinessGuideController
    participant KS as KnowledgeService
    participant KAR as KnowledgeArticleRepository
    participant KP as KnowledgeProvider

    UI->>BC: GET /api/business-guide/search?q=
    BC->>KS: search(query)
    KS->>KAR: full-text / scoring query
    KS->>KS: resolveAssistResult()
    alt Non-DatabaseKnowledgeProvider bean
        KS->>KP: assistSearch(query, articles)
    else Default
        KS->>KS: DatabaseKnowledgeProvider template summary
    end
    KS-->>UI: articles + assistSummary
```

## Provider Selection Summary

| Capability | Default (production) | Extension |
|------------|-------------------|-----------|
| Forecast insights | `RuleBasedForecastProvider` | `ForecastProvider` beans |
| Recommendations | `AIRecommendationEngine` | `AIInsightProvider` |
| Chat (AI Accountant) | Keyword templates | `AIInsightProvider.answerChat` |
| Chat (general) | Keyword templates | No provider hook |
| Categorization | `DefaultCategoryRules` | `CategorizationProvider` |
| Knowledge search | `DatabaseKnowledgeProvider` | Other `KnowledgeProvider` |
| Analytics narratives | Inline in `AnalyticsService` | `AnalyticsInsightProvider` (unused) |

## Related

- [ai-architecture.md](../ai-architecture.md)
- [ADR-001: Pluggable AI Providers](adr/001-pluggable-ai-providers.md)
- [Future LLM Integration](../../ai/future-llm-integration.md)
- [flows/forecast-flow.md](forecast-flow.md)
