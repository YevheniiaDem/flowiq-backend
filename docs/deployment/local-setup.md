# Local Setup

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | Wrapper included (`mvnw`) |
| Node.js | 18+ (frontend) |
| Docker | For PostgreSQL via Compose |

## Backend

```bash
cd flowiq-backend
# PostgreSQL starts via Spring Docker Compose integration
./mvnw spring-boot:run
```

**URL:** http://localhost:8080  
**Swagger:** http://localhost:8080/swagger-ui.html  
**Health:** http://localhost:8080/api/health

### Database Defaults

| Setting | Value |
|---------|-------|
| Host | localhost:5432 |
| Database | flowiq |
| User / Password | flowiq / flowiq123 |

### Demo Login

- Email: `demo@flowiq.ai`
- Password: `demo123`

## Frontend

```bash
cd flowiq-frontend
npm install
npm run dev
```

**URL:** http://localhost:3000

### Environment (optional)

Create `flowiq-frontend/.env.local`:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

## Verify Integration

1. Login at http://localhost:3000/login
2. Dashboard loads with stats
3. Business Guide search returns articles

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Port 8080 in use | Stop other Java process |
| Bean `taskScheduler` conflict | Use `DailyTaskScheduler` (fixed) |
| Flyway validation error | Ensure migrations V1–V8 applied |
| CORS error | Frontend must be on :3000 or :3001 |

## Related

- [Developer Handbook](../DEVELOPER_HANDBOOK.md)
- [Docker](docker.md)
