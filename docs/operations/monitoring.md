# Monitoring

## Current State

No APM or metrics exporter configured. Spring Boot Actuator not explicitly added beyond health endpoint.

## Available Today

| Signal | Source |
|--------|--------|
| Liveness | `GET /api/health` |
| Application logs | stdout (Spring default) |
| DB health | Flyway + HikariCP pool logs |
| Docker PG health | `pg_isready` in compose |

## Recommended Stack

| Tool | Purpose |
|------|---------|
| Prometheus + Micrometer | JVM, HTTP metrics |
| Grafana | Dashboards |
| Sentry | Frontend + backend errors |
| UptimeRobot | Health ping |

## Key Metrics

- HTTP 5xx rate per endpoint
- p95 latency `/api/dashboard/*`, `/api/forecasts/summary`
- Scheduler job success/failure counts
- DB connection pool utilization
- Flyway migration version

## Alerts

- Health check fails 3 consecutive times
- Error rate > 1% for 5 minutes
- Disk usage on PostgreSQL > 80%

## TODO

- [ ] Add `spring-boot-starter-actuator` with secured `/actuator/prometheus`
- [ ] Structured JSON logging
