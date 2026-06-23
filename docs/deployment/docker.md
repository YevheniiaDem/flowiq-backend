# Docker

**As-built:** 2026-06-17

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

Spring Boot can auto-start compose when `spring.docker.compose.enabled=true` (default in `application.properties`).

## Backend Application Container

**Dockerfile exists:** `flowiq-backend/Dockerfile`

Multi-stage build:
1. **Build stage:** `maven:3.9.9-eclipse-temurin-17-alpine` — `mvn package -DskipTests`
2. **Runtime stage:** `eclipse-temurin:17-jre-alpine` — runs `app.jar` as non-root user `flowiq`
3. **Healthcheck:** `GET /api/health` every 15s (90s start period)

```bash
cd flowiq-backend
docker build -t flowiq-backend .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/flowiq \
  -e SPRING_DATASOURCE_USERNAME=flowiq \
  -e SPRING_DATASOURCE_PASSWORD=flowiq123 \
  flowiq-backend
```

For container-to-container networking, use `application-docker.properties` profile or set `SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/flowiq` with a shared Docker network.

**Note:** Docker build skips tests (`-DskipTests`). Run `./mvnw verify` in CI before image publish.

## Frontend Application Container

**Dockerfile exists:** `flowiq-frontend/Dockerfile`

Multi-stage build:
1. **deps:** `npm install`
2. **builder:** `npm run build` with `NEXT_PUBLIC_API_URL` build arg
3. **runner:** Node 20 Alpine, standalone output (`next.config.ts` → `output: "standalone"`)

```bash
cd flowiq-frontend
docker build --build-arg NEXT_PUBLIC_API_URL=http://localhost:8080/api -t flowiq-frontend .
docker run -p 3000:3000 flowiq-frontend
```

## Deployment Options

| Target | Backend | Frontend |
|--------|---------|----------|
| Local dev | `./mvnw spring-boot:run` + compose PostgreSQL | `npm run dev` |
| Docker | `Dockerfile` | `Dockerfile` |
| Cloud | JAR or container (TBD) | Vercel (`flowiq.vercel.app` in CORS) or container |

**No full-stack `docker-compose.yml`** combining app + DB exists today — only PostgreSQL in `compose.yaml`.

## Related

- [Production Deployment](production-deployment.md)
- [Local Setup](local-setup.md)
- [CI/CD](ci-cd.md)
