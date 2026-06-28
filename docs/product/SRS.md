# FlowIQ — Software Requirements Specification (SRS)

| Field | Value |
|-------|-------|
| **Document ID** | FLOWIQ-SRS-001 |
| **Version** | 1.0 |
| **Date** | 2026-06-28 |
| **Status** | As-built (implementation audit) |
| **Repositories audited** | `flowiq-backend`, `flowiq-frontend`, `flowiq-automation` |

---

## 1. Introduction

### 1.1 Purpose

This Software Requirements Specification documents **existing, implemented behavior** of the FlowIQ platform as verified by code and configuration inspection across three repositories. It is intended for product, engineering, QA, and compliance stakeholders.

**This document does not specify planned or aspirational features** except where explicitly labeled **Future / Not Implemented**.

### 1.2 Scope

FlowIQ is a **financial intelligence platform for Ukrainian individual entrepreneurs (ФОП)**. The current product scope includes:

- Transaction ledger with CSV bank import
- Dashboard, analytics, and forecasting
- Rule-based AI insights (no external LLM in production)
- Tasks, notifications, and Business Guide (knowledge base)
- Report generation (PDF, Excel, CSV)
- JWT-secured REST API and Next.js SPA

**Out of scope (not implemented):** direct bank API integrations, email/Telegram/push notification delivery, multi-tenant accountant workspaces, government (ДПС) filing, OAuth login, mobile native apps.

### 1.3 System context

```
┌─────────────────────┐     HTTPS REST (/api/*)     ┌─────────────────────┐
│  flowiq-frontend    │ ◄──────────────────────────►│  flowiq-backend     │
│  Next.js 16 SPA     │   Bearer JWT + headers      │  Spring Boot 3.5    │
│  (Vercel / Docker)  │   X-App-Language/Currency   │  PostgreSQL + Flyway│
└─────────────────────┘                             └─────────────────────┘
         ▲                                                      ▲
         │ Playwright / Rest Assured                           │
         └────────────────── flowiq-automation ──────────────────┘
                              (TestNG, CI workflows)
```

| Repository | Role | Primary stack |
|------------|------|---------------|
| `flowiq-backend` | REST API, business logic, schedulers, persistence | Java 17, Spring Boot 3.5.14, PostgreSQL, Flyway |
| `flowiq-frontend` | Web UI | Next.js 16, React 19, TypeScript, Tailwind 4 |
| `flowiq-automation` | Cross-repo API/UI/E2E tests, contract validation | Java 17, TestNG, Rest Assured, Playwright |

### 1.4 Definitions

| Term | Definition |
|------|------------|
| **ФОП (FOP)** | Ukrainian sole proprietor (individual entrepreneur) |
| **ЄСВ (ESV)** | Unified social contribution |
| **FOP group** | Simplified tax group (1, 2, or 3) with annual income limits |
| **Rule-based AI** | Deterministic logic labeled as AI in UI/API; no LLM call |
| **IN_APP notification** | Notification stored in DB and shown in UI only |

### 1.5 References

| Document | Location |
|----------|----------|
| Product vision | `docs/product/vision.md` |
| Business requirements (partial) | `docs/product/business-requirements.md` |
| Backend test coverage report | `docs/qa/BACKEND_TEST_COVERAGE_REPORT.md` |
| API module docs | `docs/api/*.md` |
| Automation traceability | `flowiq-automation/docs/qa/TRACEABILITY_MATRIX.md` |

---

## 2. Business Requirements

### 2.1 Business objectives

| ID | Objective | Implemented by |
|----|-----------|----------------|
| BO-01 | Help FOP owners understand cash flow and profitability | Dashboard, analytics, transactions |
| BO-02 | Proactive tax and FOP limit compliance | Notifications, tasks, forecasts, FOP insights |
| BO-03 | Reduce need for external tax research | Business Guide (20 seeded articles, UK/EN) |
| BO-04 | Enable exportable financial reports | Reports module (PDF/Excel/CSV) |
| BO-05 | Support demo-led acquisition | `demo@flowiq.ai` seed user, transaction auto-seed |
| BO-06 | Bilingual operation (Ukrainian primary) | UK/EN UI, localized knowledge content |

### 2.2 Target users (documented personas)

| Persona | Description | Supported today |
|---------|-------------|-----------------|
| **FOP entrepreneur** | IT/services FOP, Groups 2–3 | Full self-service UI |
| **Growing FOP** | Approaching limits, VAT considerations | Forecasts, FOP limit alerts |
| **Demo user** | Trial via `demo@flowiq.ai` / `demo123` | Seeded on startup (configurable) |
| **Accountant / admin** | Multi-client management | **Not implemented** — roles exist in DB but not enforced |

### 2.3 Business rules (Ukrainian tax domain — as coded)

| Rule | Value in code | Source |
|------|---------------|--------|
| FOP group 1 annual income limit | UAH 1,672,000 | `FopProfileService`, `AnalyticsService`, `ForecastService` |
| FOP group 2 annual income limit | UAH 5,328,000 | Same |
| FOP group 3 annual income limit | UAH 7,818,000 | Same |
| Simplified tax rates (groups 1/2/3) | 10% / 5% / 3% | `SINGLE_TAX_RATES` maps in services |
| ЄСВ monthly estimate | UAH 1,760 (fixed) | `AnalyticsService`, notification/task rules |
| FOP limit alert thresholds | 70%, 85%, 95% usage | `NotificationRuleEngine` |
| Quarterly tax reminder dates | May 10, Aug 9, Nov 9, Feb 9 | `NotificationRuleEngine`, `TaskRuleEngine` |
| Default FOP group on profile create | Group 2 | `FopProfileService.getOrCreateForUser()` |
| Default KVED | 62.01 | `FopProfileService` |

> **Note:** Knowledge-base article text may describe different statutory rate ranges than the simplified constants used in calculation engines. See §16 (Ambiguities).

---

## 3. Functional Requirements

Requirements are tagged **Implemented**, **Partial**, or **Future**.

### 3.1 Authentication & sessions

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| FR-AUTH-01 | Register with email, password, name | Implemented | `POST /api/auth/register` |
| FR-AUTH-02 | Login; receive access + refresh JWT | Implemented | `POST /api/auth/login` |
| FR-AUTH-03 | Refresh access token with rotation | Implemented | `POST /api/auth/refresh`; session hash validation |
| FR-AUTH-04 | Get current user profile | Implemented | `GET /api/auth/me` |
| FR-AUTH-05 | Logout; revoke current session | Implemented | `POST /api/auth/logout` |
| FR-AUTH-06 | Frontend auto-refresh on 401 | Implemented | `flowiq-frontend/src/services/tokenRefresh.ts` |
| FR-AUTH-07 | Email verification | **Future** | `users.email_verified` column exists; no flow |
| FR-AUTH-08 | Password reset | **Future** | Frontend methods throw `not implemented` |
| FR-AUTH-09 | OAuth / social login | **Future** | Not in codebase |

### 3.2 Profile & FOP settings

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| FR-PROF-01 | View/update personal profile (name, phone) | Implemented | `GET/PUT /api/profile` |
| FR-PROF-02 | Upload avatar (JPEG/PNG/WebP/GIF, max 5 MB) | Implemented | `POST /api/profile/avatar` |
| FR-PROF-03 | Serve avatar files (public) | Implemented | `GET /api/profile/avatars/{filename}` |
| FR-PROF-04 | View/update FOP profile (group, tax system, KVED, VAT) | Implemented | `GET/PUT /api/profile/fop` |
| FR-PROF-05 | Change password | Implemented | `POST /api/profile/change-password` |
| FR-PROF-06 | List active sessions | Implemented | `GET /api/profile/sessions` |
| FR-PROF-07 | Logout current / all sessions | Implemented | `POST /api/profile/sessions/logout-*` |
| FR-PROF-08 | Settings UI for profile | Implemented | Frontend `/settings` (Profile, Security tabs) |

### 3.3 Transactions

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| FR-TXN-01 | Create/read/update/delete transactions | Implemented | `TransactionController` |
| FR-TXN-02 | Paginate, search, filter by type/date, sort | Implemented | Query params on `GET /api/transactions` |
| FR-TXN-03 | Transaction summary aggregates | Implemented | `GET /api/transactions/summary` |
| FR-TXN-04 | Types: REVENUE, EXPENSE | Implemented | `Transaction.Type` enum |
| FR-TXN-05 | User-scoped data isolation | Implemented | Services resolve user from JWT |
| FR-TXN-06 | Valid expense/income categories | Implemented | Hardcoded sets in `CategorizationEngine` |

### 3.4 CSV import

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| FR-IMP-01 | Upload CSV (multipart, max 10 MB) | Implemented | `POST /api/imports/upload` |
| FR-IMP-02 | Parse Monobank format | Implemented | `MonobankCsvStrategy` |
| FR-IMP-03 | Parse PrivatBank format | Implemented | `PrivatBankCsvStrategy` |
| FR-IMP-04 | Parse universal format (date, type, category, amount) | Implemented | `UniversalCsvStrategy` |
| FR-IMP-05 | Auto-categorize imported rows | Implemented | `CategorizationEngine` + keyword rules |
| FR-IMP-06 | Track import job status | Implemented | `import_jobs` table, list/get APIs |
| FR-IMP-07 | Notify and create review task on completion | Implemented | `NotificationGeneratorService`, `TaskGeneratorService` |
| FR-IMP-08 | Duplicate detection on import | Implemented | `TransactionRepository.existsDuplicate()` |

### 3.5 Dashboard

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| FR-DASH-01 | Stat cards (revenue, expenses, profit, cash flow) | Implemented | `GET /api/dashboard/stats` |
| FR-DASH-02 | Rule-based AI insights | Implemented | `GET /api/dashboard/insights` |
| FR-DASH-03 | Business health score (0–100) | Implemented | `GET /api/dashboard/health` |
| FR-DASH-04 | AI summary narrative | Implemented | `GET /api/dashboard/summary` |
| FR-DASH-05 | Revenue trend chart (6 months) | Implemented | `GET /api/dashboard/charts/revenue-trend` |
| FR-DASH-06 | Expense breakdown chart | Implemented | `GET /api/dashboard/charts/expense-breakdown` |
| FR-DASH-07 | Forecast snapshot widget | Implemented | `GET /api/dashboard/forecast-snapshot` |
| FR-DASH-08 | Tasks snapshot widget | Implemented | `GET /api/dashboard/tasks-snapshot` |
| FR-DASH-09 | Business Guide snapshot widget | Implemented | `GET /api/dashboard/business-guide-snapshot` |
| FR-DASH-10 | Auto-seed demo transactions if empty | Implemented | `TransactionSeedService.seedIfEmpty()` |

### 3.6 Analytics

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| FR-AN-01 | Overview (revenue, expenses, profit, tax, YoY changes) | Implemented | `GET /api/analytics/overview` |
| FR-AN-02 | Revenue trend (12 months) | Implemented | `GET /api/analytics/revenue-trend` |
| FR-AN-03 | Expense breakdown by category | Implemented | `GET /api/analytics/expense-breakdown` |
| FR-AN-04 | Profit trend | Implemented | `GET /api/analytics/profit-trend` |
| FR-AN-05 | Income vs expenses comparison | Implemented | `GET /api/analytics/income-vs-expenses` |
| FR-AN-06 | FOP-specific insights (limits, tax load) | Implemented | `GET /api/analytics/fop-insights` |

### 3.7 Forecast Center

See §12 for detailed forecast requirements.

### 3.8 AI Accountant

See §11 for detailed AI module requirements.

### 3.9 Chat

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| FR-CHAT-01 | List user conversations | Implemented | `GET /api/chat/conversations` |
| FR-CHAT-02 | Get conversation with messages | Implemented | `GET /api/chat/conversations/{id}` |
| FR-CHAT-03 | Send message; persist user + assistant replies | Implemented | `POST /api/chat/message` |
| FR-CHAT-04 | Rule-based replies (revenue, expenses, profit, etc.) | Implemented | `ChatService.generateReply()` |
| FR-CHAT-05 | Separate from AI Accountant chat | Implemented | Different API paths and persistence |

### 3.10 Tasks & deadlines

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| FR-TASK-01 | CRUD tasks | Implemented | `TaskController` |
| FR-TASK-02 | Filter by type, priority, status, due date, search | Implemented | Query params |
| FR-TASK-03 | Today / upcoming / grouped views | Implemented | `/today`, `/upcoming`, `/grouped` |
| FR-TASK-04 | Rule-based task suggestions | Implemented | `GET /api/tasks/suggestions` |
| FR-TASK-05 | Auto-generate tax/deadline tasks (daily 07:30) | Implemented | `DailyTaskScheduler`, `TaskRuleEngine` |
| FR-TASK-06 | Deduplication per user | Implemented | `deduplication_key` unique index |
| FR-TASK-07 | Tasks from imports/reports | Implemented | `TaskGeneratorService` |
| FR-TASK-08 | Calendar UI | Implemented | Frontend `TasksCalendar` |

### 3.11 Notifications

See §13 for detailed notification requirements.

### 3.12 Business Guide (Knowledge)

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| FR-BG-01 | Browse articles with pagination, category, tag filters | Implemented | `GET /api/business-guide/articles` |
| FR-BG-02 | Article detail by slug (UK/EN content) | Implemented | `GET /api/business-guide/articles/{slug}` |
| FR-BG-03 | List categories with counts | Implemented | `GET /api/business-guide/categories` |
| FR-BG-04 | Search with rule-based quick summary | Implemented | `GET /api/business-guide/search` |
| FR-BG-05 | Dashboard snapshot (popular, recent, recommended) | Implemented | `GET /api/business-guide/dashboard-snapshot` |
| FR-BG-06 | 20 seeded articles (Flyway V5) | Implemented | Migration `V5__create_knowledge_articles_table.sql` |
| FR-BG-07 | FOP eligibility checker | **Partial** | Client-side engine in frontend only; no backend API |
| FR-BG-08 | FOP groups/taxes/KVED explorer from API | **Partial** | Backend articles API exists; frontend uses **local mock data** for profile sections |

### 3.13 Reports

See §14 for detailed reporting requirements.

### 3.14 Internationalization

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| FR-I18N-01 | UI languages UK (default), EN | Implemented | Frontend `src/shared/i18n/locales/` |
| FR-I18N-02 | API localized strings via header | Implemented | `X-App-Language` → `AppPreferencesFilter` |
| FR-I18N-03 | Currency display UAH/USD/EUR | Implemented | `X-App-Currency`, `CurrencyFormatter` |
| FR-I18N-04 | Bilingual knowledge articles | Implemented | `title_uk/en`, `content_uk/en` columns |

### 3.15 Integrations (bank)

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| FR-INT-01 | Direct bank API connections | **Future** | `flowiq.features.bank-integrations-enabled=false`; flag unused in services |
| FR-INT-02 | Integrations UI | **Future** | Frontend redirects to `/coming-soon/integrations` |

### 3.16 Onboarding (frontend-only)

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| FR-ONB-01 | Product tour (driver.js) | Implemented | `flowiq-frontend/src/features/onboarding/` |
| FR-ONB-02 | Activation checklist | Implemented | `ActivationProvider` |
| FR-ONB-03 | Demo workspace overlay | Implemented | Mock dashboard data when demo mode on |
| FR-ONB-04 | What's-new modal | Implemented | Frontend component |

---

## 4. Non-Functional Requirements

### 4.1 Architecture

| ID | Requirement | Status | Details |
|----|-------------|--------|---------|
| NFR-ARCH-01 | Layered monolith backend | Implemented | Controllers → services → repositories |
| NFR-ARCH-02 | Stateless API (JWT) | Implemented | No server-side HTTP sessions |
| NFR-ARCH-03 | Schema via Flyway migrations | Implemented | `spring.jpa.hibernate.ddl-auto=validate` |
| NFR-ARCH-04 | Feature modules as packages | Implemented | forecasts, tasks, notifications, knowledge, profile |
| NFR-ARCH-05 | Pluggable AI provider interfaces | Implemented | Beans optional via `@Autowired(required=false)` |
| NFR-ARCH-06 | SPA frontend (standalone Next.js) | Implemented | `output: "standalone"` in `next.config.ts` |

### 4.2 Reliability & availability

| ID | Requirement | Status | Details |
|----|-------------|--------|---------|
| NFR-REL-01 | Health check endpoint | Implemented | `GET /api/health`, `/api/health/ping` (public) |
| NFR-REL-02 | Docker healthcheck (frontend) | Implemented | `flowiq-frontend/Dockerfile` |
| NFR-REL-03 | Graceful report failure handling | Implemented | `ReportJob.Status.FAILED`, notification on failure |
| NFR-REL-04 | Audit log async writes | Implemented | `AuditLogAsyncWriter`, thread pool (core 2, max 4) |

### 4.3 Maintainability

| ID | Requirement | Status | Details |
|----|-------------|--------|---------|
| NFR-MAIN-01 | OpenAPI documentation | Implemented | SpringDoc at `/swagger-ui.html` |
| NFR-MAIN-02 | Backend unit/integration tests | Implemented | 446 tests, JaCoCo ~81% line coverage |
| NFR-MAIN-03 | Cross-repo automation suite | Implemented | `flowiq-automation` (API, UI, E2E) |
| NFR-MAIN-04 | Correlation ID on requests | Implemented | `CorrelationIdFilter` |

### 4.4 Portability & deployment

| ID | Requirement | Status | Details |
|----|-------------|--------|---------|
| NFR-DEP-01 | Docker Compose for PostgreSQL | Implemented | `compose.yaml` (backend repo) |
| NFR-DEP-02 | Backend Docker profile | Implemented | `application-docker.properties` |
| NFR-DEP-03 | Frontend Docker image | Implemented | `flowiq-frontend/Dockerfile` |
| NFR-DEP-04 | Production frontend host | Implemented | CORS allows `https://flowiq.vercel.app` |

### 4.5 Localization & preferences

| ID | Requirement | Status | Details |
|----|-------------|--------|---------|
| NFR-L10N-01 | Request-scoped language/currency | Implemented | Thread-local `AppPreferences` |
| NFR-L10N-02 | Persisted client preferences | Implemented | `localStorage`: `flowiq_language`, `flowiq_currency` |

---

## 5. Performance Requirements

### 5.1 Documented / configured limits

| ID | Requirement | Value | Source |
|----|-------------|-------|--------|
| PERF-01 | Max upload file size | 10 MB | `spring.servlet.multipart.max-file-size` |
| PERF-02 | JWT access token lifetime | 24 hours | `jwt.access-token-expiration=86400000` |
| PERF-03 | JWT refresh token lifetime | 7 days | `jwt.refresh-token-expiration=604800000` |
| PERF-04 | CORS preflight cache | 3600 s | `CorsConfig` |
| PERF-05 | Notification list page size cap | 100 (documented in OpenAPI) | `NotificationController` |
| PERF-06 | Default task list page size | 20 | `TaskController` default |
| PERF-07 | Audit async queue capacity | 500 | `AuditAsyncConfig` |

### 5.2 Observed architectural characteristics

| ID | Characteristic | Status | Notes |
|----|----------------|--------|-------|
| PERF-08 | Report generation | Synchronous | Runs in HTTP request despite `GENERATING` status |
| PERF-09 | HTTP caching | Not implemented | No `@Cacheable`, no CDN cache headers on API |
| PERF-10 | Redis / distributed cache | Not implemented | — |
| PERF-11 | Background job queue | Not implemented | Schedulers only for daily rules |

> **Future:** Async report generation and caching are described in architecture docs as evolution targets but are **not implemented**.

---

## 6. Security Requirements

### 6.1 Authentication

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| SEC-01 | Password hashing (BCrypt) | Implemented | Spring Security `PasswordEncoder` |
| SEC-02 | JWT access tokens (HS256) | Implemented | `JwtService`, jjwt 0.12.6 |
| SEC-03 | Refresh token rotation with session binding | Implemented | `UserSession.refresh_token_hash` |
| SEC-04 | Only access tokens accepted by filter | Implemented | `JwtAuthenticationFilter.isAccessToken()` |
| SEC-05 | Invalid JWT clears security context | Implemented | Filter catch block |

### 6.2 Authorization

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| SEC-06 | Public routes explicitly configured | Implemented | `SecurityConfig` permitAll list |
| SEC-07 | All other `/api/**` require authentication | Implemented | `.anyRequest().authenticated()` |
| SEC-08 | User data scoped by authenticated user ID | Implemented | Service-layer email → user lookup |
| SEC-09 | Role-based access (ADMIN, USER, VIEWER) | **Partial** | Roles stored in JWT; **no `@PreAuthorize` or URL rules** |
| SEC-10 | Multi-tenant isolation | **Future** | Single-user model only |

### 6.3 Transport & CORS

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| SEC-11 | CORS allowlist | Implemented | localhost:3000/3001, Docker frontend, flowiq.vercel.app |
| SEC-12 | CSRF disabled (stateless JWT API) | Implemented | `SecurityConfig` |

### 6.4 Data protection

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| SEC-13 | Avatar path traversal prevention | Implemented | `AvatarStorageService.resolveAvatarPath()` |
| SEC-14 | Audit metadata sanitization | Implemented | `AuditMetadataSanitizer` |
| SEC-15 | Refresh tokens stored hashed | Implemented | Session service |
| SEC-16 | Tokens in browser localStorage | Implemented | Frontend (XSS exposure surface) |
| SEC-17 | Dev JWT secret in properties file | Implemented | Must be overridden in production |

### 6.5 Audit logging

| ID | Requirement | Status | Implementation |
|----|-------------|--------|----------------|
| SEC-18 | Append-only audit log table | Implemented | Flyway V6, `audit_log` |
| SEC-19 | Auth events logged | Implemented | Register, login, logout, refresh, failures |
| SEC-20 | Financial action audit (transactions, reports, imports) | Implemented | `@Auditable` on selected controllers |
| SEC-21 | Profile/password/session audit | Implemented | Profile and preference controllers |
| SEC-22 | Task/chat/notification read audit | **Future** | Enum values exist; not wired to controllers |
| SEC-23 | Audit retention / purge job | **Partial** | Config present; `purge-enabled=false`, no scheduler |

---

## 7. Accessibility Requirements

### 7.1 Frontend (as implemented)

| ID | Requirement | Status | Notes |
|----|-------------|--------|-------|
| A11Y-01 | Semantic HTML landmarks | Partial | `<main>` in layout; limited landmark strategy |
| A11Y-02 | ARIA labels on interactive controls | Partial | Chat send, notification bell, settings, theme toggle |
| A11Y-03 | Focus-visible styles | Partial | shadcn/ui `focus-visible:ring-*` |
| A11Y-04 | Screen reader text (`sr-only`) | Partial | Dialog close, date inputs |
| A11Y-05 | Form validation ARIA | Partial | `aria-invalid` on inputs |
| A11Y-06 | Keyboard navigation | Partial | Native focus; no documented skip links |
| A11Y-07 | WCAG 2.x conformance target | **Not specified** | No audit tooling in CI |
| A11Y-08 | Color contrast certification | **Not verified** | Theme supports light/dark |

### 7.2 Backend API accessibility

API is consumed by SPA; no direct end-user accessibility surface beyond error message clarity (`ErrorResponse` JSON).

---

## 8. API Requirements

### 8.1 General conventions

| Convention | Value |
|------------|-------|
| Base path | `/api` |
| Auth header | `Authorization: Bearer <access_token>` |
| Language header | `X-App-Language` (`uk`, `en`) |
| Currency header | `X-App-Currency` (`UAH`, `USD`, `EUR`) |
| Error format | `{ status, message, timestamp, errors? }` — `ErrorResponse` |
| Validation errors | HTTP 400, `message: "Validation failed"`, field map |
| Pagination | `page` (0-based), `size` query params |
| OpenAPI | `/v3/api-docs`, `/swagger-ui.html` |

### 8.2 Endpoint inventory

#### Public (no JWT)

| Method | Path |
|--------|------|
| GET | `/api/health`, `/api/health/ping` |
| POST | `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh` |
| GET | `/api/profile/avatars/{filename}` |
| GET | `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**` |

#### Authenticated (JWT required)

| Module | Base path | Operations |
|--------|-----------|------------|
| Auth | `/api/auth` | `GET /me`, `POST /logout` |
| Profile | `/api/profile` | CRUD profile, FOP, avatar, password, sessions |
| Transactions | `/api/transactions` | CRUD, summary, filters |
| Imports | `/api/imports` | upload, list, getById |
| Dashboard | `/api/dashboard` | stats, insights, health, summary, charts, snapshots |
| Analytics | `/api/analytics` | overview, trends, FOP insights |
| Forecasts | `/api/forecasts` | revenue, expenses, profit, taxes, fop-limit, summary |
| AI Accountant | `/api/ai-accountant` | health, recommendations, tax-advisor, forecasts, chat |
| Chat | `/api/chat` | conversations, message |
| Tasks | `/api/tasks` | CRUD, today, upcoming, grouped, suggestions |
| Notifications | `/api/notifications` | list, unread-count, summary, read, delete |
| Notification prefs | `/api/settings/notifications` | get, update, reset |
| Reports | `/api/reports` | list, preview, generate, getById, download |
| Business Guide | `/api/business-guide` | articles, categories, search, dashboard-snapshot |

**Total:** 15 controllers, ~90 endpoints.

### 8.3 API versioning

No URL version prefix (e.g. `/api/v1`). Breaking changes would require coordinated frontend/backend deployment.

### 8.4 Rate limiting

**Not implemented** in backend code.

---

## 9. Database Requirements

### 9.1 Database management

| ID | Requirement | Status | Details |
|----|-------------|--------|---------|
| DB-01 | PostgreSQL as primary store | Implemented | Version 15+ (Compose); Testcontainers uses 16 |
| DB-02 | Flyway migrations only (no Hibernate DDL) | Implemented | V1–V8 |
| DB-03 | Validate schema on startup | Implemented | `ddl-auto=validate` |

### 9.2 Migration history

| Version | Description |
|---------|-------------|
| V1 | Core schema: users, transactions, chat, import_jobs, report_jobs |
| V2 | `transactions.auto_categorized` |
| V3 | `notifications` + deduplication unique constraint |
| V4 | `tasks` + deduplication partial unique index |
| V5 | `knowledge_articles` + 20 seed articles |
| V6 | `audit_log` (append-only, JSONB metadata) |
| V7 | Profile extensions, `fop_profiles`, `user_sessions` |
| V8 | `notification_preferences` |

### 9.3 Entity model (summary)

| Entity | Table | Key relationships |
|--------|-------|-------------------|
| User | `users` | 1:N transactions, conversations; role enum |
| Transaction | `transactions` | N:1 User |
| ImportJob | `import_jobs` | userId (no FK in V1) |
| ReportJob | `report_jobs` | userId; binary `file_content` |
| ChatConversation / ChatMessage | `chat_*` | N:1 User |
| Notification | `notifications` | userId |
| Task | `tasks` | N:1 User |
| KnowledgeArticle | `knowledge_articles` | Standalone |
| FopProfile | `fop_profiles` | 1:1 User |
| UserSession | `user_sessions` | N:1 User |
| NotificationPreference | `notification_preferences` | N:1 User |
| AuditLog | `audit_log` | Optional actor FK |

### 9.4 Data integrity notes

- `import_jobs.user_id` and early tables lack FK constraints (V1 design).
- Report files stored as BYTEA in PostgreSQL (size growth consideration).
- User email unique constraint enforced at application/DB level.

---

## 10. AI Module Requirements

### 10.1 Implemented (rule-based)

| ID | Capability | API / component | Logic type |
|----|------------|-----------------|------------|
| AI-01 | Dashboard AI insights | `DashboardService.getInsights()` | Threshold rules |
| AI-02 | Dashboard AI summary | `DashboardService.getAISummary()` | Template strings |
| AI-03 | AI Accountant health score | `GET /api/ai-accountant/health` | Weighted scoring |
| AI-04 | AI recommendations | `GET /api/ai-accountant/recommendations` | `AIRecommendationEngine` |
| AI-05 | Tax advisor narrative | `GET /api/ai-accountant/tax-advisor` | Rule templates |
| AI-06 | AI Accountant forecasts | `GET /api/ai-accountant/forecasts` | Simple projection math |
| AI-07 | AI Accountant chat | `POST /api/ai-accountant/chat` | Keyword matching |
| AI-08 | Chat assistant (separate module) | `POST /api/chat/message` | Keyword matching + DB history |
| AI-09 | Transaction auto-categorization | `CategorizationEngine` | Keyword rules + optional provider |
| AI-10 | Knowledge search assist | `DatabaseKnowledgeProvider` | Scoring + template summary |
| AI-11 | Forecast insights/warnings | `RuleBasedForecastProvider` | Trend/FOP/tax rules |
| AI-12 | Analytics insight extensions | `AnalyticsInsightProvider` interface | **No implementations** |

### 10.2 Extension interfaces (future LLM integration)

| Interface | Package | Default implementation | Future use |
|-----------|---------|------------------------|------------|
| `AIInsightProvider` | `aiaccountant` | None (empty list) | External LLM recommendations |
| `ForecastProvider` | `forecasts.provider` | `RuleBasedForecastProvider` only | LLM forecast narratives |
| `CategorizationProvider` | `categorization` | None | ML/LLM categorization |
| `KnowledgeProvider` | `knowledge.provider` | `DatabaseKnowledgeProvider` | LLM-augmented search |
| `AnalyticsInsightProvider` | `analytics` | None | Extended analytics narratives |

> **Labeling requirement:** UI and OpenAPI describe features as "AI-powered." Per this SRS, production behavior is **deterministic rule-based logic** unless a provider bean is added.

### 10.3 AI-related audit

| Event | Status |
|-------|--------|
| `AI_ACCOUNTANT_CHAT` | Audited via `@Auditable` on controller |
| `CHAT_MESSAGE_SEND` | Defined in enum; **not wired** |

---

## 11. Notification Requirements

### 11.1 Notification model

| Field | Values |
|-------|--------|
| **Types** | `TAX`, `FOP_LIMIT`, `FINANCIAL`, `AI_INSIGHT`, `REPORT`, `SYSTEM` |
| **Severities** | `INFO`, `SUCCESS`, `WARNING`, `CRITICAL` |
| **Channels (schema)** | `IN_APP`, `EMAIL`, `PUSH`, `TELEGRAM` |
| **Channel (production)** | **`IN_APP` only** |

### 11.2 User-facing operations

| ID | Requirement | Status | API |
|----|-------------|--------|-----|
| NOTIF-01 | Paginated notification feed | Implemented | `GET /api/notifications` |
| NOTIF-02 | Filter by unread, type, severity | Implemented | Query params |
| NOTIF-03 | Unread count (badge) | Implemented | `GET /api/notifications/unread-count` |
| NOTIF-04 | Summary statistics | Implemented | `GET /api/notifications/summary` |
| NOTIF-05 | Mark single read | Implemented | `PUT /api/notifications/{id}/read` |
| NOTIF-06 | Mark all read | Implemented | `PUT /api/notifications/read-all` |
| NOTIF-07 | Delete notification | Implemented | `DELETE /api/notifications/{id}` |
| NOTIF-08 | Deep link via `action_url` | Implemented | Field on entity; UI navigation |

### 11.3 Generation rules (automated)

| Trigger | Source | Examples |
|---------|--------|----------|
| Daily scheduler (08:00) | `NotificationScheduler` → `NotificationRuleEngine` | FOP limit 70/85/95%, tax deadlines, expense spike, revenue drop |
| Task rules | `TaskRuleEngine` | Linked notifications for tax/ESV tasks |
| Import lifecycle | `ImportService` | Processing, completed, partial, failed |
| Report lifecycle | `ReportsService` | Completed, failed, format-specific (PDF/Excel) |
| Task creation | `TaskGeneratorService` | Optional notification on task create |

### 11.4 Preferences

| ID | Requirement | Status | Details |
|----|-------------|--------|---------|
| NOTIF-P01 | 24 preference keys in 5 categories | Implemented | `NotificationPreferenceKey` enum |
| NOTIF-P02 | Get/update/reset preferences | Implemented | `/api/settings/notifications` |
| NOTIF-P03 | Gate in-app creation by preference | Implemented | `NotificationPreferenceService.isInAppEnabled()` |
| NOTIF-P04 | EMAIL/PUSH/TELEGRAM delivery | **Future** | Stored in DB; no senders |

### 11.5 Deduplication

Unique constraint on `(user_id, deduplication_key)` prevents duplicate automated notifications.

---

## 12. Forecast Requirements

### 12.1 Forecast Center API

| ID | Requirement | Status | Endpoint |
|----|-------------|--------|----------|
| FC-01 | Revenue forecast with trend | Implemented | `GET /api/forecasts/revenue` |
| FC-02 | Expense forecast | Implemented | `GET /api/forecasts/expenses` |
| FC-03 | Profit forecast | Implemented | `GET /api/forecasts/profit` |
| FC-04 | Tax forecast cards | Implemented | `GET /api/forecasts/taxes` |
| FC-05 | FOP income limit projection | Implemented | `GET /api/forecasts/fop-limit` |
| FC-06 | Combined summary with insights/warnings | Implemented | `GET /api/forecasts/summary` |

### 12.2 Forecast engine

| Parameter | Value | Source |
|-----------|-------|--------|
| Forecast horizons | 1, 3, 6, 12 months | `ForecastEngine.FORECAST_HORIZONS` |
| Rolling window | 3 months | Engine constants |
| Trend window | 6 months | Engine constants |
| Projection method | Trend-based growth on historical monthly totals | `ForecastEngine` |
| Minimum history | Graceful defaults when insufficient data | Service layer |

### 12.3 Insights and warnings

| Component | Responsibility |
|-----------|----------------|
| `RuleBasedForecastProvider.generateInsights()` | Revenue/expense/FOP/tax trend narratives |
| `RuleBasedForecastProvider.generateWarnings()` | FOP limit risk, revenue decline, expense growth |
| `ForecastService.getSnapshot()` | Dashboard widget subset |

### 12.4 AI Accountant forecasts (separate)

`GET /api/ai-accountant/forecasts` uses **different calculation** in `AIAccountantService` (3/6/12-month simplified horizons). Not identical to Forecast Center engine output.

### 12.5 Future

| ID | Requirement | Status |
|----|-------------|--------|
| FC-F01 | LLM-generated forecast narratives | Future — `ForecastProvider` extension |
| FC-F02 | ML-based anomaly detection | Future — referenced in docs only |

---

## 13. Reporting Requirements

### 13.1 Report types

| Type | Enum value |
|------|------------|
| Profit and loss | `PROFIT_AND_LOSS` |
| Cash flow | `CASH_FLOW` |
| Revenue summary | `REVENUE_SUMMARY` |
| Expense summary | `EXPENSE_SUMMARY` |
| Tax summary | `TAX_SUMMARY` |
| FOP summary | `FOP_SUMMARY` |

### 13.2 Output formats

| Format | Extension | Renderer |
|--------|-----------|----------|
| PDF | `.pdf` | `OpenPdfReportRenderer` (Unicode via DejaVu fonts) |
| Excel | `.xlsx` | `PoiReportRenderer` |
| CSV | `.csv` | Inline in `ReportFileGenerator` |

### 13.3 Operations

| ID | Requirement | Status | API |
|----|-------------|--------|-----|
| RPT-01 | List user's reports | Implemented | `GET /api/reports` |
| RPT-02 | Preview data for date range | Implemented | `GET /api/reports/preview` |
| RPT-03 | Period presets | Implemented | `THIS_MONTH`, `LAST_MONTH`, `QUARTER`, `YEAR` |
| RPT-04 | Generate report | Implemented | `POST /api/reports/generate` |
| RPT-05 | Poll job status | Implemented | `GET /api/reports/{id}` |
| RPT-06 | Download file | Implemented | `GET /api/reports/{id}/download` |
| RPT-07 | Store file in database | Implemented | `report_jobs.file_content` BYTEA |
| RPT-08 | Notify on completion/failure | Implemented | In-app notification + review task |
| RPT-09 | Async background generation | **Partial** | Status `GENERATING` set but work is **synchronous** in request thread |

### 13.4 Report content

Includes revenue, expenses, profit, tax burden, category breakdowns, monthly lines, and FOP/tax section when profile data available (`ReportData` builder in `ReportsService`).

---

## 14. Frontend Requirements (Summary)

| ID | Requirement | Status |
|----|-------------|--------|
| FE-01 | 16 authenticated routes + login/register | Implemented |
| FE-02 | Client-side route protection | Implemented (`MainLayout` auth check) |
| FE-03 | JWT in localStorage with refresh | Implemented |
| FE-04 | Feature-module architecture | Implemented (`src/features/*`) |
| FE-05 | API client with i18n headers | Implemented (`src/services/api.ts`) |
| FE-06 | Vitest unit tests | **Partial** — exist locally; **not in CI** |
| FE-07 | Tax profile from backend | **Partial** — mock service used |
| FE-08 | Business Guide static sections | **Partial** — hybrid API + local locale data |
| FE-09 | Global search in top nav | **Partial** — input present; no search action |
| FE-10 | Bank integrations page | **Future** — coming-soon redirect |

---

## 15. Automation & QA Requirements

| ID | Requirement | Status | Repository |
|----|-------------|--------|------------|
| QA-01 | Backend unit/controller/integration tests | Implemented | `flowiq-backend` (446 tests) |
| QA-02 | API smoke/regression/contract tests | Implemented | `flowiq-automation` |
| QA-03 | UI smoke/regression (Playwright) | Implemented | `flowiq-automation` |
| QA-04 | E2E user journeys (11 classes) | Implemented | `flowiq-automation` |
| QA-05 | PR validation (backend unit + contract) | Implemented | `flowiq-automation` PR workflow |
| QA-06 | Nightly full-stack regression | Implemented | Docker stack + parallel suites |
| QA-07 | Frontend tests in CI | **Not implemented** | `frontend-ci.yml`: lint + build only |
| QA-08 | Cypress | **Not used** | Playwright only |

---

## 16. Requirements Gap Analysis

### 16.1 Missing requirements (documented intent vs implementation)

| Gap | Documented expectation | Actual state |
|-----|------------------------|--------------|
| GAP-01 | Refresh token endpoint (BR-01.5 marked missing) | **Implemented** — docs stale |
| GAP-02 | Live tax profile API (BR-06.4) | Backend FOP API exists; **frontend tax profile still uses mock** |
| GAP-03 | Email/Telegram notifications (BR-08.4) | Channel enum only; IN_APP delivery |
| GAP-04 | Role-based admin APIs | Roles in DB/JWT; no enforcement |
| GAP-05 | Bank integrations feature flag | Property exists; **never read by services** |
| GAP-06 | FOP eligibility checker API (BR-07.6) | Frontend-only client engine |
| GAP-07 | Async report jobs (BR-09.2 implied) | Synchronous generation |
| GAP-08 | Email verification / password reset | Schema/frontend stubs only |
| GAP-09 | Rate limiting / abuse protection | Not specified or implemented |
| GAP-10 | API versioning policy | Not defined |

### 16.2 Ambiguous behavior

| ID | Area | Ambiguity |
|----|------|-----------|
| AMB-01 | **Tax rates** | Code uses 10%/5%/3% single tax; knowledge articles may state different statutory ranges |
| AMB-02 | **ЄСВ amount** | Fixed ₴1,760/month; not tied to minimum wage updates |
| AMB-03 | **"AI" labeling** | Marketing/UI implies ML; all production logic is rule-based |
| AMB-04 | **Two chat systems** | `/api/chat` vs `/api/ai-accountant/chat` — different persistence, similar keyword logic |
| AMB-05 | **Two forecast outputs** | Forecast Center vs AI Accountant forecasts use different algorithms |
| AMB-06 | **Transaction auto-seed** | New users get 6 months demo transactions silently on first dashboard/analytics access |
| AMB-07 | **Demo user seed default** | `flowiq.demo-seed.enabled` defaults to **true** (`matchIfMissing`) |
| AMB-08 | **Report job status** | `GENERATING` implies async; completes in same HTTP request |
| AMB-09 | **Notification channels** | Preferences UI may imply EMAIL/PUSH; only IN_APP is delivered |
| AMB-10 | **Category validation** | CSV/manual categories must match hardcoded sets or fall back to "Other" |

### 16.3 Undocumented features (exist in code, weak/absent in product docs)

| Feature | Location | Notes |
|---------|----------|-------|
| Session management API | `ProfileController` `/sessions/*` | Not in older API overview docs |
| Refresh token rotation | `AuthService.refresh()` | BR doc still says missing |
| Correlation ID filter | `CorrelationIdFilter` | Ops concern |
| Audit async writer | `AuditLogAsyncWriter` | Partial audit enum coverage |
| App preference headers | `AppPreferencesFilter` | Undocumented in some API client guides |
| Import review tasks | `TaskGeneratorService.createImportReviewTask()` | Side effect of import |
| Report review tasks | `TaskGeneratorService.createReportReviewTask()` | Side effect of report |
| Product tour / activation | Frontend onboarding module | No backend counterpart |
| FOP eligibility checker | Frontend `checker/` package | No SRS in product docs |
| Demo workspace mode | Frontend `demoWorkspaceData.ts` | Overlays mock dashboard |
| Cross-browser test profiles | `flowiq-automation` Maven profiles | Firefox/WebKit |
| AI helper Maven profiles | `flowiq-automation` | Requirements traceability agents |

---

## 17. Future Features (Explicitly Not Implemented)

The following appear in roadmap, ADRs, interfaces, or UI placeholders but are **not production functionality**:

| Feature | Evidence | Target hook |
|---------|----------|-------------|
| OpenAI / Claude / Gemini integration | `docs/ai/future-llm-integration.md`, provider interfaces | `AIInsightProvider`, etc. |
| Email / Push / Telegram notifications | `NotificationChannel` enum | Delivery services |
| Bank API integrations (Monobank/Privat API) | `BANK_INTEGRATIONS_ROADMAP.md`, coming-soon UI | `FeatureFlags.bankIntegrationsEnabled` |
| OAuth2 / Google login | Roadmap | — |
| Multi-tenant accountant workspace | Roles exist; no UI/API | `User.Role.ADMIN/VIEWER` |
| Government ДПС API filing | Roadmap | — |
| Mobile native app | Roadmap | API-first design |
| Password reset / email verification | Frontend throws | — |
| Async report job queue | Architecture notes | — |
| Redis caching | Architecture notes | — |
| Audit log purge scheduler | Config only | `flowiq.audit.purge-enabled` |
| Production rate limiting | — | — |

---

## 18. Traceability Matrix (High Level)

| Business req | Backend module | Frontend route | Automation coverage |
|--------------|----------------|----------------|---------------------|
| BO-01 Cash flow | transactions, dashboard | `/`, `/transactions` | E2E + API |
| BO-02 Compliance | notifications, tasks, forecasts | `/notifications`, `/tasks`, `/forecasts` | E2E + API |
| BO-03 Knowledge | business-guide | `/business-guide` | API + UI smoke |
| BO-04 Reports | reports | `/reports` | E2E |
| BO-05 Demo | DemoUserSeed, TransactionSeed | demo login | Smoke |
| BO-06 i18n | AppPreferences, knowledge i18n | PreferencesContext | Partial |

Detailed mapping: `flowiq-automation/docs/qa/TRACEABILITY_MATRIX.md`.

---

## 19. Document Maintenance

| Trigger | Action |
|---------|--------|
| New REST endpoint | Update §8.2 and module §3 |
| New Flyway migration | Update §9.2 |
| LLM provider wired | Update §10 (move from Future) |
| Frontend mock removed | Update §16.1 gaps |
| CI workflow change | Update §15 |

**Known stale documents to reconcile:** `docs/product/business-requirements.md` (BR-01.5, BR-06.4), `docs/database/migrations.md` (V6–V8), `docs/product/roadmap.md` (test/CI status), `docs/qa/test-strategy.md` (test counts).

---

*End of SRS — generated from implementation audit, 2026-06-28.*
