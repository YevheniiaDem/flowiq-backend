# Logging

## Backend

Spring Boot default logging (Logback).

**Notable log sources:**
- `com.flowiq` — application
- `org.flywaydb` — migration status
- `org.hibernate.SQL` — enabled via `spring.jpa.show-sql=true` (dev)

### Scheduler Logs

`DailyTaskScheduler`, `NotificationScheduler` log warnings on per-user generation failures.

### Recommended Production Format

```json
{"timestamp":"...","level":"INFO","logger":"com.flowiq.tasks","message":"...","userId":"1"}
```

## Frontend

Browser console only. No centralized error reporting.

## Sensitive Data

Do not log:
- JWT tokens
- Passwords
- Full CSV file contents

## TODO

- [ ] Correlation ID in `AppPreferencesFilter`
- [ ] Request logging filter (exclude health)
