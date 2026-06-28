# Deployment Architecture

**As-built:** 2026-06-28  
**Scope:** Runtime topology for local, Docker, and production targets

## Deployment Diagram

```mermaid
flowchart TB
    subgraph Users
        BROWSER[Browser]
    end

    subgraph Production["Production (target)"]
        VERCEL[Vercel / CDN<br/>flowiq.vercel.app]
        BE_HOST[Backend host<br/>JAR or container :8080]
        PG_PROD[(PostgreSQL<br/>managed or self-hosted)]
    end

    subgraph LocalDev["Local Development"]
        FE_DEV[next dev :3000]
        BE_DEV[spring-boot:run :8080]
        PG_DEV[(Docker Compose<br/>postgres:5432)]
    end

    subgraph DockerOptional["Docker (optional)"]
        FE_IMG[flowiq-frontend image :3000]
        BE_IMG[flowiq-backend image :8080]
        PG_IMG[postgres:15-alpine]
    end

    BROWSER --> VERCEL & FE_DEV & FE_IMG
    VERCEL & FE_DEV & FE_IMG -->|HTTPS REST /api| BE_HOST & BE_DEV & BE_IMG
    BE_HOST & BE_DEV & BE_IMG --> PG_PROD & PG_DEV & PG_IMG
```

## Component Deployment Matrix

| Component | Local dev | Docker | Production |
|-----------|-----------|--------|------------|
| **Frontend** | `npm run dev` | `flowiq-frontend/Dockerfile` standalone | Vercel or container |
| **Backend** | `./mvnw spring-boot:run` | `flowiq-backend/Dockerfile` | JAR or container |
| **PostgreSQL** | `compose.yaml` auto-start | `postgres:15-alpine` | Managed DB |
| **Flyway** | On backend startup | On backend startup | On backend startup |
| **Automation** | Local Maven | Nightly CI Docker stack | GitHub Actions only |

## Network & Ports

| Service | Port | Protocol |
|---------|------|----------|
| Frontend | 3000 | HTTP(S) |
| Backend API | 8080 | HTTP(S) |
| PostgreSQL | 5432 | TCP (internal) |
| Swagger UI | 8080/swagger-ui.html | HTTP (dev/staging) |

## Environment Configuration

### Backend (`application.properties`)

| Property | Dev default | Production |
|----------|-------------|------------|
| `spring.datasource.url` | `localhost:5432/flowiq` | Secrets / env var |
| `jwt.secret` | Dev placeholder | **Must override** |
| `spring.docker.compose.enabled` | `true` | `false` |
| `flowiq.demo-seed.enabled` | `true` (matchIfMissing) | Disable in prod |

Docker profile: `application-docker.properties` — JDBC host `postgres`.

### Frontend

| Variable | Purpose |
|----------|---------|
| `NEXT_PUBLIC_API_URL` | Backend base URL (build-time) |
| Default | `http://localhost:8080/api` |

## CORS Topology

Backend `CorsConfig` allowlist:

- `http://localhost:3000`, `http://localhost:3001`
- `https://flowiq.vercel.app`
- Docker frontend hostname (when used)

```mermaid
flowchart LR
    FE[Frontend origin] -->|Preflight OPTIONS| CORS[CorsConfig]
    CORS -->|Allow + credentials| BE[Backend /api]
```

## Docker Images

### Backend Dockerfile

```mermaid
flowchart LR
    A[maven:3.9 temurin-17] -->|mvn package -DskipTests| B[app.jar]
    B --> C[eclipse-temurin:17-jre-alpine]
    C --> D[HEALTHCHECK /api/health]
```

### Frontend Dockerfile

```mermaid
flowchart LR
    A[node:20 deps] --> B[npm run build standalone]
    B --> C[node:20-alpine runner]
    C --> D[:3000 server.js]
```

## Compose Layout

**Current:** `flowiq-backend/compose.yaml` — PostgreSQL only.

**Nightly CI:** `flowiq-automation` provides full-stack `docker-compose` for regression (backend + frontend + postgres).

No production full-stack compose in backend repo.

## Data Flow at Runtime

```mermaid
sequenceDiagram
    participant U as User
    participant FE as Frontend
    participant BE as Backend
    participant PG as PostgreSQL

    U->>FE: HTTPS page load
    FE->>BE: API + JWT
    BE->>PG: JDBC queries
    PG-->>BE: Rows
    BE-->>FE: JSON
    FE-->>U: Rendered UI
```

## Health Checks

| Target | Endpoint | Consumer |
|--------|----------|----------|
| Backend | `GET /api/health` | Docker HEALTHCHECK, automation wait script |
| Backend ping | `GET /api/health/ping` | Lightweight liveness |
| Frontend | HTTP :3000 | Docker HEALTHCHECK |

## CD Status

**Continuous deployment is not automated.** CI builds and tests; deployment is manual (Vercel, JAR, or Docker push).

See [cicd-architecture.md](cicd-architecture.md).

## Related

- [Docker deployment guide](../deployment/docker.md)
- [Production deployment](../deployment/production-deployment.md)
- [Environments](../deployment/environments.md)
- [Local setup](../deployment/local-setup.md)
