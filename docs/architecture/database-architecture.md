# Database Architecture

**Engine:** PostgreSQL 15  
**ORM:** Hibernate / Spring Data JPA (`ddl-auto=validate`)  
**Migrations:** Flyway (`classpath:db/migration`)

## Connection (Development)

| Property | Value |
|----------|-------|
| URL | `jdbc:postgresql://localhost:5432/flowiq` |
| User | `flowiq` |
| Password | `flowiq123` |
| Docker | `compose.yaml` → `postgres:15-alpine` |

## Schema Evolution Strategy

- **No** Hibernate auto-DDL in production path (`validate` only)
- All schema changes via versioned Flyway scripts: `V{n}__{description}.sql`
- Partial unique indexes for nullable deduplication keys

## Tables (9)

| Table | Migration | Rows Owner |
|-------|-----------|------------|
| `users` | V1 | Core |
| `transactions` | V1, V2 | Per user |
| `chat_conversations`, `chat_messages` | V1 | Per user |
| `import_jobs` | V1 | Per user |
| `report_jobs` | V1 | Per user |
| `notifications` | V3 | Per user |
| `tasks` | V4 | Per user |
| `knowledge_articles` | V5 | Global (shared content) |

## Indexing Strategy

- Foreign keys: `user_id` on all user-scoped tables
- Query patterns: `(user_id, status)`, `(user_id, is_read)`, `due_date`, `updated_at DESC`
- Knowledge: `category`, `published_at DESC`
- Deduplication: partial unique `(user_id, deduplication_key) WHERE deduplication_key IS NOT NULL`

## Multi-Tenancy Model

- **Row-level isolation** via `user_id` (or `User` FK on `transactions`)
- No schema-per-tenant
- Knowledge articles are **global** (not per-user)

## BYTEA Storage

`report_jobs.file_content` stores generated PDF/Excel/CSV in-database (suitable for MVP; consider object storage at scale).

## Related Documents

- [Schema Overview](../database/schema-overview.md)
- [Entities](../database/entities.md)
- [ER Diagram](../database/er-diagram.md)
- [Migrations](../database/migrations.md)
