# Smoke Checklist

**Duration target:** 15 minutes post-deploy.

## Infrastructure
- [ ] `GET /api/health` → 200 `"status":"UP"`
- [ ] `GET /api/health/ping` → `pong`
- [ ] PostgreSQL connected (no Flyway errors in logs)
- [ ] Frontend loads (no 500 on `/`)

## Auth
- [ ] `POST /api/auth/login` demo credentials → token
- [ ] `GET /api/auth/me` with token → user object

## Core API (with token)
- [ ] `GET /api/dashboard/stats` → 200
- [ ] `GET /api/transactions?page=0&size=5` → 200
- [ ] `GET /api/forecasts/summary` → 200
- [ ] `GET /api/tasks/today` → 200
- [ ] `GET /api/notifications/unread-count` → 200
- [ ] `GET /api/business-guide/articles?page=0&size=5` → 200

## UI Smoke
- [ ] Login page → dashboard redirect
- [ ] Sidebar navigation (3 random routes)
- [ ] Logout → login page

## Fail Criteria

Stop release if:
- Health check fails
- Login fails
- Any core GET above returns 5xx
- Flyway migration failed on startup
