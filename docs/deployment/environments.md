# Environments

| Environment | Frontend | Backend | Database |
|-------------|----------|---------|----------|
| **Local** | localhost:3000 | localhost:8080 | Docker PostgreSQL |
| **Staging** | TBD | TBD | Managed PostgreSQL |
| **Production** | flowiq.vercel.app (CORS allowed) | TBD | Managed PostgreSQL |

## Configuration by Environment

| Variable | Local | Production |
|----------|-------|------------|
| `jwt.secret` | Dev default in properties | Secrets manager |
| `spring.datasource.url` | localhost:5432 | Cloud DB URL |
| `NEXT_PUBLIC_API_URL` | localhost:8080/api | Production API URL |
| `spring.jpa.show-sql` | true | false |

## Feature Flags

None implemented. Demo seed runs in all environments unless disabled.

## TODO

- [ ] Separate `application-prod.properties`
- [ ] Disable `DemoUserSeedService` in production
- [ ] Staging environment with anonymized data
