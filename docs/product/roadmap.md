# Product Roadmap

Roadmap derived from implemented features and documented extension points in the codebase.

## Shipped (Current)

| Area | Status | Key Components |
|------|--------|----------------|
| Auth (JWT) | ✅ | `AuthController`, `JwtService` |
| Transactions CRUD | ✅ | `TransactionController`, CSV import |
| Dashboard | ✅ | Stats, charts, AI summary, snapshots |
| Analytics | ✅ | FOP insights, trends |
| Forecast Center 2.0 | ✅ | `ForecastController`, `ForecastEngine` |
| Tasks & Deadlines | ✅ | `TaskController`, `DailyTaskScheduler` |
| Notifications | ✅ | `NotificationController`, `NotificationScheduler` |
| Business Guide / Knowledge | ✅ | `BusinessGuideController`, V5 migration |
| AI Accountant (rule-based) | ✅ | `AIAccountantController` |
| Reports (PDF/Excel/CSV) | ✅ | `ReportsController` |
| Chat | ✅ | `ChatController` |
| Frontend (16 routes) | ✅ | Next.js App Router |

## Near Term (Extension Points Ready)

| Initiative | Code Hook | Notes |
|------------|-----------|-------|
| OpenAI / Claude forecast insights | `ForecastProvider` | Implement as `@Component` bean |
| LLM knowledge answers | `KnowledgeProvider` | Non-`DatabaseKnowledgeProvider` takes priority |
| LLM transaction categorization | `CategorizationProvider` | After `DefaultCategoryRules` |
| LLM dashboard insights | `AIInsightProvider` | Used by `AIAccountantService` |
| Refresh token flow | `JwtService.generateRefreshToken` | Frontend `refreshToken()` not implemented |
| Tax profile API | `taxProfileService` | Frontend still uses mock |
| Bank integrations API | `integrationsService` + `FeatureFlags` | **Planned** — UI hidden, see [Bank Integrations Roadmap](../roadmap/BANK_INTEGRATIONS_ROADMAP.md) |
| Email / Telegram notifications | `Notification.channel` | Enum exists; only IN_APP used |

## Medium Term

| Initiative | Dependency |
|------------|------------|
| Multi-user / accountant workspace | Role model exists (`ADMIN`, `USER`, `VIEWER`) — no UI |
| Automated tests (API + E2E) | No test suite in repos today |
| CI/CD pipelines | Not configured in repo |
| Production Dockerfile | Backend has `compose.yaml` only |
| OAuth2 / Google login | Not implemented |
| Mobile app | API-first design supports future client |

## Long Term

| Initiative | Vision |
|------------|--------|
| Bank API integrations | Beyond CSV import — see [Bank Integrations Roadmap](../roadmap/BANK_INTEGRATIONS_ROADMAP.md) |
| Government API (ДПС) | Declaration filing |
| Marketplace for accountants | B2B2C channel |
| Predictive FOP group migration | ML on income trends |

## Future Roadmap — Bank Integrations

**Status: Planned**

Flowiq currently works through **imported bank statements** only. Direct bank connections are not available.

| Planned Feature | Phase |
|-----------------|-------|
| Monobank Integration | Phase 2 |
| PrivatBank Integration | Phase 3 |
| Open Banking Support | Phase 5 |
| Automatic Synchronization | Phase 2–4 |
| Real-time Transaction Sync | Phase 5 |

Full architecture: [docs/roadmap/BANK_INTEGRATIONS_ROADMAP.md](../roadmap/BANK_INTEGRATIONS_ROADMAP.md).

## TODO (Undocumented / Incomplete in Code)

- [ ] `ChatController` OpenAPI annotations
- [ ] Refresh token endpoint and rotation
- [ ] Backend `/dashboard/tax-profile` endpoint
- [ ] Bank integration connect/disconnect APIs (see [Bank Integrations Roadmap](../roadmap/BANK_INTEGRATIONS_ROADMAP.md))
- [ ] Automated test coverage
- [ ] Production secrets management (Vault / env injection)

## Related Documents

- [Vision](vision.md)
- [Future LLM Integration](../ai/future-llm-integration.md)
- [Bank Integrations Roadmap](../roadmap/BANK_INTEGRATIONS_ROADMAP.md)
- [Coverage Report](../COVERAGE-REPORT.md)
