# Health Checks

## Endpoints

| Endpoint | Auth | Response |
|----------|------|----------|
| `GET /api/health` | Public | `HealthResponse` with status UP |
| `GET /api/health/ping` | Public | `"pong"` |

### Example

```json
{
  "status": "UP",
  "message": "Flowiq Backend is running successfully",
  "version": "0.0.1-SNAPSHOT",
  "timestamp": "2026-06-11T14:00:00",
  "environment": "development"
}
```

## Implicit Health Signals

| Check | How |
|-------|-----|
| Database | App starts only if Flyway + Hikari succeed |
| JWT | Protected endpoint 401 without token |

## Kubernetes Probes (Future)

```yaml
livenessProbe:
  httpGet:
    path: /api/health/ping
    port: 8080
readinessProbe:
  httpGet:
    path: /api/health
    port: 8080
```

## Smoke Integration

See [Smoke Checklist](../qa/smoke-checklist.md).
