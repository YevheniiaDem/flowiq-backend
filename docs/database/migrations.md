# Flyway Migrations

**Location:** `src/main/resources/db/migration/`  
**Config:** `spring.flyway.enabled=true`

## History

| Version | File | Description |
|---------|------|-------------|
| V1 | `V1__initial_schema.sql` | users, transactions, chat, import_jobs, report_jobs |
| V2 | `V2__add_auto_categorized_column.sql` | `transactions.auto_categorized` |
| V3 | `V3__create_notifications_table.sql` | notifications + indexes |
| V4 | `V4__create_tasks_table.sql` | tasks + dedup index |
| V5 | `V5__create_knowledge_articles_table.sql` | knowledge_articles + 20 seed articles |

## V1 Tables Created

- `users` — email unique index
- `transactions` — indexes on `user_id`, `transaction_date`
- `chat_conversations`, `chat_messages`
- `import_jobs`
- `report_jobs` — includes `file_content BYTEA`

## V3 Notable Indexes

```sql
CREATE UNIQUE INDEX uk_notifications_user_dedup ON notifications (user_id, deduplication_key);
CREATE INDEX idx_notifications_user_read ON notifications (user_id, is_read);
```

## V4 Partial Unique Index

```sql
CREATE UNIQUE INDEX uk_tasks_user_dedup ON tasks (user_id, deduplication_key)
    WHERE deduplication_key IS NOT NULL;
```

## V5 Seed Data

20 articles covering: FOP groups, taxes, ESV, military tax, declarations, KVED, accounting, FAQ, legal changes (2025–2026).

## Next Migration

Suggested: `V6__...` for tax profile table, integration credentials, or audit log.

## Related

- [Schema Overview](schema-overview.md)
