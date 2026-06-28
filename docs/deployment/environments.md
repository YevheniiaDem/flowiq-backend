# Environments

| Environment | Frontend | Backend | Database |
|-------------|----------|---------|----------|
| **Local** | localhost:3000 | localhost:8080 | Docker PostgreSQL (`compose.yaml`) |
| **Docker** | Container :3000 | Container :8080 (`SPRING_PROFILES_ACTIVE=docker`) | Linked PostgreSQL |
| **Production** | flowiq.vercel.app (CORS allowed) | JAR or container | Managed PostgreSQL |

## Configuration by environment

| Variable | Local (default) | Docker profile | Production (`prod` profile) |
|----------|-----------------|----------------|----------------------------|
| `jwt.secret` | Dev default in properties | `${JWT_SECRET}` env | `${JWT_SECRET}` **required** (validated) |
| `jwt.access-token-expiration` | 24 h | 24 h (unless overridden) | 15 min default |
| `spring.datasource.*` | localhost / flowiq123 | `${SPRING_DATASOURCE_*}` | Env vars **required** |
| `spring.jpa.show-sql` | true | false | false |
| `flowiq.demo-seed.enabled` | true (default) | false | false |
| Swagger UI | enabled | disabled | disabled |
| `NEXT_PUBLIC_API_URL` | localhost:8080/api | Build arg | Production API URL |

Production properties: `src/main/resources/application-prod.properties`  
Docker properties: `src/main/resources/application-docker.properties`

## Feature flags

| Property | Default | Purpose |
|----------|---------|---------|
| `flowiq.features.bank-integrations-enabled` | `false` | Bank API integrations (not implemented) |
| `flowiq.demo-seed.enabled` | `true` (dev) | Seeds `demo@flowiq.ai` on startup |

## Related

- [Developer Handbook §3 — Environment variables](../DEVELOPER_HANDBOOK.md#3-environment-variables)
- [Production deployment](production-deployment.md)
- [Security audit](../security/SECURITY_AUDIT.md)
