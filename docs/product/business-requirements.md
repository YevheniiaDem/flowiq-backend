# Business Requirements

Functional requirements mapped to implemented modules.

## BR-01: User Authentication

| ID | Requirement | Implementation | Status |
|----|-------------|----------------|--------|
| BR-01.1 | Register with email/password | `POST /api/auth/register` | ✅ |
| BR-01.2 | Login and receive JWT | `POST /api/auth/login` | ✅ |
| BR-01.3 | Access protected API with Bearer token | `JwtAuthenticationFilter` | ✅ |
| BR-01.4 | Logout | `POST /api/auth/logout` | ✅ |
| BR-01.5 | Refresh token rotation | `JwtService.generateRefreshToken` | ⚠️ Token issued; no refresh endpoint |

## BR-02: Transaction Management

| ID | Requirement | Implementation | Status |
|----|-------------|----------------|--------|
| BR-02.1 | CRUD transactions | `TransactionController` | ✅ |
| BR-02.2 | Filter/search/paginate | Query params on `GET /api/transactions` | ✅ |
| BR-02.3 | Import bank CSV | `ImportController`, Monobank/PrivatBank parsers | ✅ |
| BR-02.4 | Auto-categorize on import | `CategorizationEngine` | ✅ |

## BR-03: Dashboard Analytics

| ID | Requirement | Implementation | Status |
|----|-------------|----------------|--------|
| BR-03.1 | Stat cards (revenue, expenses, profit) | `GET /api/dashboard/stats` | ✅ |
| BR-03.2 | Revenue/expense charts | Chart endpoints | ✅ |
| BR-03.3 | Business health score | `GET /api/dashboard/health` | ✅ |
| BR-03.4 | AI summary narrative | `GET /api/dashboard/summary` | ✅ Rule-based |
| BR-03.5 | Forecast snapshot widget | `GET /api/dashboard/forecast-snapshot` | ✅ |
| BR-03.6 | Tasks snapshot widget | `GET /api/dashboard/tasks-snapshot` | ✅ |
| BR-03.7 | Business Guide snapshot | `GET /api/dashboard/business-guide-snapshot` | ✅ |

## BR-04: Revenue Forecasting

| ID | Requirement | Implementation | Status |
|----|-------------|----------------|--------|
| BR-04.1 | Revenue/expense/profit forecast | `ForecastController` | ✅ |
| BR-04.2 | Tax burden forecast | `GET /api/forecasts/taxes` | ✅ |
| BR-04.3 | FOP income limit projection | `GET /api/forecasts/fop-limit` | ✅ |
| BR-04.4 | Trend analysis & insights | `ForecastEngine`, `RuleBasedForecastProvider` | ✅ |
| BR-04.5 | LLM-generated insights | `ForecastProvider` interface | 🔌 Extension point |

## BR-05: Task Automation

| ID | Requirement | Implementation | Status |
|----|-------------|----------------|--------|
| BR-05.1 | Manual task CRUD | `TaskController` | ✅ |
| BR-05.2 | Auto-generate tax/deadline tasks | `TaskRuleEngine`, `DailyTaskScheduler` | ✅ |
| BR-05.3 | Tasks from notifications/imports | `TaskGeneratorService` | ✅ |
| BR-05.4 | Calendar and grouped views | Frontend `TasksView` | ✅ |
| BR-05.5 | Deduplication | `deduplication_key` unique per user | ✅ |

## BR-06: Tax & FOP Monitoring

| ID | Requirement | Implementation | Status |
|----|-------------|----------------|--------|
| BR-06.1 | FOP limit usage alerts | `NotificationRuleEngine` at 70/85/95% | ✅ |
| BR-06.2 | Tax deadline notifications | Notification + task rules | ✅ |
| BR-06.3 | FOP insights in analytics | `GET /api/analytics/fop-insights` | ✅ |
| BR-06.4 | Live tax profile from backend | — | ❌ Frontend mock only |

## BR-07: Knowledge Center (Business Guide)

| ID | Requirement | Implementation | Status |
|----|-------------|----------------|--------|
| BR-07.1 | Article storage (UK/EN) | `knowledge_articles` table, 20 seeds | ✅ |
| BR-07.2 | Category browse & filter | `GET /api/business-guide/articles` | ✅ |
| BR-07.3 | Smart search + AI summary | `GET /api/business-guide/search` | ✅ Rule-based |
| BR-07.4 | Legal updates section | `LEGAL_CHANGES` category | ✅ |
| BR-07.5 | SEO article URLs | `/business-guide/articles/[slug]` | ✅ |
| BR-07.6 | FOP eligibility checker API | Client-side only | ⚠️ |

## BR-08: Notifications

| ID | Requirement | Implementation | Status |
|----|-------------|----------------|--------|
| BR-08.1 | In-app notification feed | `NotificationController` | ✅ |
| BR-08.2 | Unread count & mark read | API + `NotificationBell` | ✅ |
| BR-08.3 | Deep links to modules | `action_url` field | ✅ |
| BR-08.4 | Email/Telegram delivery | Channel enum | ❌ Not wired |

## BR-09: Reporting

| ID | Requirement | Implementation | Status |
|----|-------------|----------------|--------|
| BR-09.1 | Generate PDF/Excel/CSV | `ReportsService`, POI, OpenPDF | ✅ |
| BR-09.2 | Async job status | `report_jobs` table | ✅ |
| BR-09.3 | Download completed report | `GET /api/reports/{id}/download` | ✅ |

## BR-10: Internationalization

| ID | Requirement | Implementation | Status |
|----|-------------|----------------|--------|
| BR-10.1 | UK/EN UI | `src/shared/i18n/locales/` | ✅ |
| BR-10.2 | UK/EN knowledge content | `title_uk/en`, `content_uk/en` | ✅ |
| BR-10.3 | Currency display UAH/USD/EUR | `PreferencesContext`, `CurrencyFormatter` | ✅ |

## Related Documents

- [Vision](vision.md)
- [Modules](../modules/)
- [Coverage Report](../COVERAGE-REPORT.md)
