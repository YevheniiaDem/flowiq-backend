# Documentation Coverage Report

**Generated:** 2026-06-11  
**Updated:** 2026-06-17 (architecture audit)  
**Method:** Full codebase analysis (backend + frontend)  
**Documentation location:** `flowiq-backend/docs/`

## Summary

| Area | Files Created | Coverage | Notes |
|------|---------------|----------|-------|
| Product | 4/4 | **95%** | Roadmap includes TODOs for unbuilt features |
| Architecture | 12/12 | **94%** | C4, data-sources, ADR-001–008 (2026-06-17) |
| API | 7/7 | **85%** | Transactions/Imports/Reports/Analytics/Chat summarized in modules; no dedicated API md |
| Database | 5/5 | **95%** | Full V1–V5 documented |
| Modules | 10/10 | **90%** | AI Accountant & Chat lighter detail |
| AI | 5/5 | **90%** | LLM section is planned architecture |
| Frontend | 5/5 | **85%** | Per-component props not exhaustively listed |
| QA | 5/5 | **80%** | Backend unit tests exist; no integration/E2E |
| Security | 4/4 | **85%** | Role enforcement gaps documented |
| Deployment | 5/5 | **85%** | GitHub Actions CI (backend + frontend); CD not implemented |
| Operations | 4/4 | **75%** | APM not implemented |
| Legal | 2/2 | **50%** | Drafts only — require legal review |

**Overall documentation coverage: ~85%**

## Module Coverage Detail

| Module | Backend | Frontend | API Doc | DB | Overall |
|--------|---------|----------|---------|-----|---------|
| Authentication | ✅ | ✅ | ✅ | ✅ users | **95%** |
| Transactions | ✅ | ✅ | ⚠️ module only | ✅ | **90%** |
| Dashboard | ✅ | ✅ | ✅ | — | **95%** |
| Forecast Center | ✅ | ✅ | ✅ | — | **95%** |
| Tasks Center | ✅ | ✅ | ✅ | ✅ | **95%** |
| Notifications | ✅ | ✅ | ✅ | ✅ | **95%** |
| Business Guide | ✅ | ✅ | ✅ | ✅ | **95%** |
| Analytics | ✅ | ✅ | ⚠️ | — | **85%** |
| Reports | ✅ | ✅ | ⚠️ | ✅ | **85%** |
| AI Accountant | ✅ | ✅ | ⚠️ | — | **80%** |
| Chat | ✅ | ✅ | ❌ | ✅ | **70%** |
| Imports | ✅ | ✅ | ⚠️ | ✅ | **85%** |
| Integrations | ❌ planned | hidden coming-soon | ❌ | ❌ | **Planned** — see [Bank Integrations Roadmap](roadmap/BANK_INTEGRATIONS_ROADMAP.md) |
| Settings | — | ✅ | — | — | **60%** |

## Undocumented / Partial Areas (TODO)

### Backend
- [ ] `ChatController` — missing OpenAPI annotations
- [ ] `TransactionController` — dedicated `api/transactions-api.md`
- [ ] `ImportController`, `ReportsController`, `AnalyticsController` — dedicated API pages
- [ ] Refresh token endpoint
- [ ] Email/Telegram notification delivery
- [ ] `Role`-based authorization enforcement

### Frontend
- [ ] `taxProfileService` — still mock; document when API exists
- [ ] `businessGuideService` partial mock (groups, taxes, KVED)
- [ ] `integrationsService` — enable when `BANK_INTEGRATIONS_ENABLED` (Phase 2+)
- [ ] `checkerService` — document as client-side engine (partial in business-guide module)
- [ ] Next.js middleware for auth (not implemented)

### Infrastructure
- [x] Dockerfile for backend (`flowiq-backend/Dockerfile`)
- [x] Dockerfile for frontend (`flowiq-frontend/Dockerfile`)
- [x] CI pipelines — GitHub Actions (`backend-ci.yml`, `frontend-ci.yml`)
- [ ] CD / automated deploy
- [ ] Staging environment
- [ ] Application metrics / Actuator
- [ ] Full-stack docker-compose (app + DB)

### Testing
- [x] Backend unit tests — 9 classes, JaCoCo (see [UNIT-TEST-COVERAGE.md](UNIT-TEST-COVERAGE.md))
- [ ] Backend integration tests (Testcontainers)
- [ ] Frontend automated tests (Vitest/Playwright)

## Files Created

Total: **65+ markdown files** in `docs/` tree (including C4, data-sources, AI architecture docs added 2026-06-17).

## Maintenance

Update documentation when:
1. New Flyway migration added
2. New controller endpoint added
3. New frontend route added
4. AI provider implementation added
5. Auth or deployment architecture changes

## Related

- [Documentation Index](index.md)
