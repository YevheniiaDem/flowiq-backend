# ER Diagram

> **Canonical ER diagram:** [architecture/database-er-diagram.md](../architecture/database-er-diagram.md)

The full entity-relationship diagram with all 13 tables (Flyway V1–V8) lives in the architecture folder.

## Quick reference (core entities)

```mermaid
erDiagram
    users ||--o{ transactions : "user_id"
    users ||--o{ notifications : "user_id"
    users ||--o{ tasks : "user_id"
    users ||--o| fop_profiles : "user_id"
    chat_conversations ||--o{ chat_messages : "conversation_id"
```

## Related

- [Database ER Diagram](../architecture/database-er-diagram.md)
- [Database Architecture](../architecture/database-architecture.md)
- [Entities](entities.md)
- [Relationships](relationships.md)
