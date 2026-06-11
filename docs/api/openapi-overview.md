# OpenAPI Overview

## Access

| Resource | URL (local) |
|----------|-------------|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Public group | `/api/health/**`, `/api/auth/register`, `/api/auth/login` |

**Library:** springdoc-openapi 2.8.17  
**Config:** `com.flowiq.config.OpenApiConfig`

## Authentication

Default security scheme: **HTTP Bearer JWT** (`bearerAuth`).

```http
Authorization: Bearer <access_token>
```

Obtain token via `POST /api/auth/login`.

## Standard Error Response

`ErrorResponse` via `GlobalExceptionHandler`:

```json
{
  "status": 404,
  "message": "Article not found: unknown-slug",
  "timestamp": "2026-06-11T12:00:00",
  "errors": { "email": "must not be blank" }
}
```

`@ApiErrorResponses` on controllers documents 400, 401, 403, 404, 500.

## Controllers Summary

| Controller | Base Path | Endpoints | OpenAPI |
|------------|-----------|-----------|---------|
| `AuthController` | `/api/auth` | 4 | ✅ |
| `HealthController` | `/api/health` | 2 | ✅ |
| `TransactionController` | `/api/transactions` | 6 | ✅ |
| `ImportController` | `/api/imports` | 3 | ✅ |
| `DashboardController` | `/api/dashboard` | 9 | ✅ |
| `AnalyticsController` | `/api/analytics` | 6 | ✅ |
| `AIAccountantController` | `/api/ai-accountant` | 5 | ✅ |
| `ChatController` | `/api/chat` | 3 | ⚠️ No annotations |
| `ReportsController` | `/api/reports` | 5 | ✅ |
| `NotificationController` | `/api/notifications` | 6 | ✅ |
| `TaskController` | `/api/tasks` | 9 | ✅ |
| `ForecastController` | `/api/forecasts` | 6 | ✅ |
| `BusinessGuideController` | `/api/business-guide` | 5 | ✅ |

## App Preference Headers

| Header | Example | Purpose |
|--------|---------|---------|
| `X-App-Language` | `uk`, `en` | Localized knowledge content |
| `X-App-Currency` | `UAH` | Currency formatting |

## Detailed API Docs

- [Authentication](authentication-api.md)
- [Dashboard](dashboard-api.md)
- [Forecast](forecast-api.md)
- [Tasks](tasks-api.md)
- [Knowledge](knowledge-api.md)
- [Notifications](notification-api.md)

## TODO

- [ ] Add OpenAPI annotations to `ChatController`
- [ ] Add `/api/tasks`, `/api/forecasts`, `/api/business-guide` to protected OpenAPI group
