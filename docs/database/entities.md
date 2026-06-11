# Database Entities

JPA entity reference mapped to PostgreSQL tables.

## users

**Entity:** `com.flowiq.entity.User`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| email | VARCHAR(100) | UNIQUE, NOT NULL |
| password | VARCHAR | BCrypt hash |
| name | VARCHAR(100) | |
| company | VARCHAR(100) | Optional |
| role | VARCHAR | ADMIN, USER, VIEWER |
| is_active | BOOLEAN | Default true |
| email_verified | BOOLEAN | Default false |
| avatar_url | VARCHAR(500) | |
| created_at, updated_at | TIMESTAMP | Auto |

## transactions

**Entity:** `com.flowiq.entity.Transaction`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| user_id | BIGINT | FK → users |
| type | VARCHAR | REVENUE, EXPENSE |
| amount | DECIMAL(15,2) | |
| category | VARCHAR(100) | |
| description | VARCHAR(255) | |
| transaction_date | DATE | |
| auto_categorized | BOOLEAN | V2 migration |
| created_at, updated_at | TIMESTAMP | |

## notifications

**Entity:** `com.flowiq.notifications.entity.Notification`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| user_id | BIGINT | |
| title | VARCHAR(255) | |
| message | VARCHAR(1000) | |
| type | VARCHAR(20) | TAX, FOP_LIMIT, etc. |
| severity | VARCHAR(20) | INFO..CRITICAL |
| channel | VARCHAR(20) | Default IN_APP |
| is_read | BOOLEAN | |
| action_url | VARCHAR(255) | Frontend deep link |
| deduplication_key | VARCHAR(255) | UK with user_id |
| created_at, read_at, expires_at | TIMESTAMP | |

## tasks

**Entity:** `com.flowiq.tasks.entity.Task`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| user_id | BIGINT | FK → users |
| title | VARCHAR(255) | |
| description | VARCHAR(1000) | |
| type | VARCHAR(20) | TAX, REPORTING, etc. |
| priority | VARCHAR(20) | Default MEDIUM |
| status | VARCHAR(20) | Default TODO |
| due_date | DATE | |
| completed_at | TIMESTAMP | |
| deduplication_key | VARCHAR(255) | Partial unique index |
| created_at, updated_at | TIMESTAMP | |

## knowledge_articles

**Entity:** `com.flowiq.knowledge.entity.KnowledgeArticle`

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL | PK |
| slug | VARCHAR(120) | UNIQUE |
| title_uk, title_en | VARCHAR(255) | |
| category | VARCHAR(30) | KnowledgeCategory enum |
| content_uk, content_en | TEXT | |
| summary_uk, summary_en | VARCHAR(500) | |
| impact_uk, impact_en | VARCHAR(500) | Legal changes |
| tags_uk, tags_en | VARCHAR(500) | Comma-separated |
| published_at | DATE | |
| view_count | INTEGER | |
| created_at, updated_at | TIMESTAMP | |

## import_jobs / report_jobs / chat_*

See migration V1 in [migrations.md](migrations.md).

## Related

- [Relationships](relationships.md)
