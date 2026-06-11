# Entity Relationships

## User-Centric Model

```mermaid
erDiagram
    users ||--o{ transactions : owns
    users ||--o{ chat_conversations : owns
    users ||--o{ import_jobs : owns
    users ||--o{ report_jobs : owns
    users ||--o{ notifications : receives
    users ||--o{ tasks : has

    chat_conversations ||--o{ chat_messages : contains

    users {
        bigint id PK
        varchar email UK
    }

    transactions {
        bigint id PK
        bigint user_id FK
        varchar type
        decimal amount
    }

    tasks {
        bigint id PK
        bigint user_id FK
        varchar deduplication_key
    }

    notifications {
        bigint id PK
        bigint user_id FK
        varchar deduplication_key UK
    }
```

## Knowledge (Independent)

`knowledge_articles` has **no FK to users** — global content library.

## JPA Mapping Notes

| Entity | User Reference |
|--------|----------------|
| `Transaction` | `@ManyToOne User` |
| `Task`, `Notification` | Flat `Long userId` |
| `ImportJob`, `ReportJob` | Flat `Long userId` |

## Cascade Rules

- `ChatConversation` → `ChatMessage`: orphan removal on conversation delete
- Transactions: no cascade from user delete (FK constraint)

## Related

- [ER Diagram](er-diagram.md)
