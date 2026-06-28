# Database Architecture

**As-built:** 2026-06-28  
**Engine:** PostgreSQL 15  
**ORM:** Hibernate / Spring Data JPA (`ddl-auto=validate`)  
**Migrations:** Flyway (`classpath:db/migration`) — **V1 through V8**

> ER diagram: [database-er-diagram.md](database-er-diagram.md)

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

## Tables (13)

| Table | Migration | Scope |
|-------|-----------|-------|
| `users` | V1, V7 | Core identity |
| `fop_profiles` | V7 | 1:1 user FOP settings |
| `user_sessions` | V7 | Refresh token sessions |
| `transactions` | V1, V2 | Per user |
| `chat_conversations`, `chat_messages` | V1 | Per user |
| `import_jobs` | V1 | Per user |
| `report_jobs` | V1 | Per user (BYTEA files) |
| `notifications` | V3 | Per user |
| `tasks` | V4 | Per user |
| `knowledge_articles` | V5 | Global (shared content) |
| `audit_log` | V6 | Append-only compliance |
| `notification_preferences` | V8 | Per user channel toggles |

## Indexing Strategy

- Foreign keys: `user_id` on all user-scoped tables
- Query patterns: `(user_id, status)`, `(user_id, is_read)`, `due_date`, `updated_at DESC`
- Knowledge: `category`, `published_at DESC`
- Deduplication: partial unique `(user_id, deduplication_key) WHERE deduplication_key IS NOT NULL`
- Audit: `created_at`, `actor_user_id`, `event_type`

## Multi-Tenancy Model

- **Row-level isolation** via `user_id` (or `User` FK on `transactions`)
- No schema-per-tenant
- Knowledge articles are **global** (not per-user)

## BYTEA Storage

`report_jobs.file_content` stores generated PDF/Excel/CSV in-database (suitable for MVP; consider object storage at scale).

## Related Documents

- [Database ER Diagram](database-er-diagram.md)
- [Schema Overview](../database/schema-overview.md)
- [Entities](../database/entities.md)
- [Migrations](../database/migrations.md)
