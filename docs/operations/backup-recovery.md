# Backup & Recovery

## PostgreSQL (Development)

Docker volume `postgres-data` in compose.

### Manual Backup

```bash
docker exec flowiq-postgres pg_dump -U flowiq flowiq > backup.sql
```

### Restore

```bash
docker exec -i flowiq-postgres psql -U flowiq flowiq < backup.sql
```

## Production Recommendations

| Item | Strategy |
|------|----------|
| Database | Daily automated snapshots (RDS/Cloud SQL) |
| Point-in-time recovery | Enable WAL archiving |
| Report BYTEA | Migrate to S3 with versioning |
| Retention | 30 days minimum |

## Disaster Recovery

**RTO target:** 4 hours  
**RPO target:** 1 hour (with PITR)

### Recovery Steps

1. Provision new PostgreSQL instance
2. Restore latest snapshot
3. Run pending Flyway migrations if needed
4. Deploy backend with same `jwt.secret` (or force re-login)
5. Verify smoke checklist

## Knowledge Articles

Seed data in V5 migration can rebuild articles; user data cannot.

## Related

- [Migrations](../database/migrations.md)
