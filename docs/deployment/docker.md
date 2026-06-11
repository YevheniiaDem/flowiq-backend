# Docker

## PostgreSQL (compose.yaml)

**File:** `flowiq-backend/compose.yaml`

```yaml
services:
  postgres:
    image: postgres:15-alpine
    container_name: flowiq-postgres
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: flowiq
      POSTGRES_USER: flowiq
      POSTGRES_PASSWORD: flowiq123
    volumes:
      - postgres-data:/var/lib/postgresql/data
```

### Manual Start

```bash
cd flowiq-backend
docker compose up -d
```

Spring Boot auto-starts compose when `spring.docker.compose.enabled=true`.

## Application Container

**No Dockerfile** exists in the repository today.

### Recommended Production Dockerfile (TODO)

```dockerfile
# TODO: Multi-stage build
# Stage 1: eclipse-temurin:17-jdk — mvn package
# Stage 2: eclipse-temurin:17-jre — java -jar app.jar
# ENV: SPRING_DATASOURCE_URL, JWT_SECRET
```

## Frontend Container

Not containerized. Deployed to Vercel per CORS config.

## Related

- [Production Deployment](production-deployment.md)
