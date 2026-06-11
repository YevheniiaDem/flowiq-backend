# Schema Overview

PostgreSQL database `flowiq` — 9 tables, 5 Flyway migrations.

## Table Summary

| Table | Purpose | Scoped By |
|-------|---------|-----------|
| `users` | Authentication & profile | Global |
| `transactions` | Revenue/expense ledger | `user_id` |
| `chat_conversations` | AI chat threads | `user_id` |
| `chat_messages` | Chat messages | `conversation_id` |
| `import_jobs` | CSV import history | `user_id` |
| `report_jobs` | Generated reports (incl. BYTEA) | `user_id` |
| `notifications` | In-app alerts | `user_id` |
| `tasks` | Tasks & deadlines | `user_id` |
| `knowledge_articles` | Business Guide content | Global (shared) |

## Conventions

- Primary keys: `BIGSERIAL`
- Timestamps: `TIMESTAMP(6) WITHOUT TIME ZONE`
- Enums stored as `VARCHAR` (JPA `@Enumerated(STRING)`)
- FK naming: `fk_{table}_{ref}`

## Related

- [Entities](entities.md)
- [Relationships](relationships.md)
- [Migrations](migrations.md)
- [ER Diagram](er-diagram.md)
