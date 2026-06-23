# Architect Interview Guide — FlowIQ

**Prepared:** 2026-06-23  
**Source of truth:** `flowiq-backend` + `flowiq-frontend` code  
**Companion docs:** [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md), [ARCHITECT_REVIEW_CHEAT_SHEET.md](ARCHITECT_REVIEW_CHEAT_SHEET.md), [ADR_DEFENSE_GUIDE.md](ADR_DEFENSE_GUIDE.md)

> **Note:** Frontend paths use `flowiq-frontend/` — sibling repository to `flowiq-backend`.

---

## How to use

Each question includes five parts: **краткий ответ**, **подробный ответ**, **код**, **документация**, **ADR**. Answers reflect the codebase as of the audit date, not roadmap aspirations.

---

# Backend (Q1–Q5)

### Q1. Какая архитектурная модель используется на backend?

**Краткий ответ:** Классическая **layered architecture**: Controller → Service → Repository → Entity, с отдельными engines/providers для доменной логики.

**Подробный ответ:** Spring Boot 3.5 / Java 17. HTTP-слой — 13 REST-контроллеров под `/api/*`. Бизнес-логика в `@Service`. Доступ к данным — Spring Data JPA repositories. Rule-based engines (`ForecastEngine`, `AIRecommendationEngine`, `TaskRuleEngine`, `NotificationRuleEngine`, `CategorizationEngine`) и provider-интерфейсы (ADR-001) вынесены из контроллеров. Глобальные ошибки — `GlobalExceptionHandler`. Планировщики (`@Scheduled`) вызывают rule engines напрямую.

**Код:**
- `src/main/java/com/flowiq/FlowiqBackendApplication.java`
- `src/main/java/com/flowiq/controller/` (13 controllers)
- `src/main/java/com/flowiq/service/`
- `src/main/java/com/flowiq/repository/`

**Документация:**
- [backend-architecture.md](backend-architecture.md)
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) — Backend section

**ADR:**
- [007-layered-architecture.md](adr/007-layered-architecture.md)

---

### Q2. Сколько REST API модулей и как они сгруппированы?

**Краткий ответ:** **13 контроллеров**: auth, health, dashboard, transactions, analytics, forecasts, AI accountant, chat, imports, reports, tasks, notifications, business guide.

**Подробный ответ:** Все защищённые маршруты требуют JWT (`SecurityConfig`). Публичные: `/api/health/**`, `/api/auth/register`, `/api/auth/login`, Swagger. Каждый модуль соответствует frontend-странице (см. cross-stack map в каталоге). `DashboardController` агрегирует данные из нескольких сервисов (stats, insights, widgets, knowledge snapshot).

**Код:**
- `src/main/java/com/flowiq/controller/DashboardController.java`
- `src/main/java/com/flowiq/controller/TransactionController.java`
- `src/main/java/com/flowiq/controller/AIAccountantController.java`

**Документация:**
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) — Controllers table
- [data-sources.md](data-sources.md)

**ADR:**
- [007-layered-architecture.md](adr/007-layered-architecture.md)
- [008-frontend-architecture.md](adr/008-frontend-architecture.md)

---

### Q3. Как обеспечивается multi-tenancy (изоляция данных пользователей)?

**Краткий ответ:** **Application-level isolation** по `user_id` из JWT (`UserPrincipal`); не schema-per-tenant.

**Подробный ответ:** Сервисы получают текущего пользователя из `SecurityContextHolder` и фильтруют запросы по `userId`. Пример: `TransactionService` использует `findByIdAndUserId`, Criteria API с `cb.equal(root.get("user").get("id"), userId)`. FK `transactions.user_id → users.id` в V1. `knowledge_articles` — глобальный каталог без `user_id`.

**Код:**
- `src/main/java/com/flowiq/security/UserPrincipal.java`
- `src/main/java/com/flowiq/service/TransactionService.java` — `getCurrentUserEntity()`, `findOwnedTransaction()`
- `src/main/resources/db/migration/V1__initial_schema.sql` — FK на transactions

**Документация:**
- [database-architecture.md](database-architecture.md)
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) — Relationships

**ADR:**
- [004-postgresql-selection.md](adr/004-postgresql-selection.md)
- [006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md)

---

### Q4. Что делает `TransactionSeedService` и где он вызывается?

**Краткий ответ:** При пустом счёте пользователя **автоматически создаёт 6 месяцев синтетических транзакций**; вызывается из 7+ сервисов.

**Подробный ответ:** `seedIfEmpty(User)` проверяет `existsByUserId`; если данных нет — генерирует income/expense по шаблону `MONTHLY_REVENUE_TARGETS`. Если данные есть — `ensureSixMonthHistory` дополняет историю. Вызывается из `DashboardService`, `AnalyticsService`, `ForecastService`, `ReportsService`, `ChatService`, `AIAccountantService`, `TaskService` перед чтением метрик. **Нет feature flag** для отключения в production (TD-C01).

**Код:**
- `src/main/java/com/flowiq/service/TransactionSeedService.java`
- `src/main/java/com/flowiq/service/DashboardService.java` — вызов seed

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) — TD-C01
- [data-sources.md](data-sources.md)

**ADR:**
- [002-transaction-seed-strategy.md](adr/002-transaction-seed-strategy.md)

---

### Q5. Как обрабатываются ошибки API?

**Краткий ответ:** Централизованный `@ControllerAdvice` — `GlobalExceptionHandler` мапит исключения в `ErrorResponse` с HTTP-кодами.

**Подробный ответ:** Кастомные исключения: `BadRequestException` (400), `UnauthorizedException` (401), `ResourceNotFoundException` (404). Validation errors — `MethodArgumentNotValidException`. Необработанные — 500 с generic message. OpenAPI аннотации `@ApiErrorResponses` на контроллерах документируют стандартные ответы.

**Код:**
- `src/main/java/com/flowiq/exception/GlobalExceptionHandler.java`
- `src/main/java/com/flowiq/exception/BadRequestException.java`
- `src/main/java/com/flowiq/config/openapi/ApiErrorResponses.java`

**Документация:**
- [backend-architecture.md](backend-architecture.md)

**ADR:**
- [007-layered-architecture.md](adr/007-layered-architecture.md)

---

# Frontend (Q6–Q10)

### Q6. Какой стек и версии frontend?

**Краткий ответ:** **Next.js 16**, **React 19**, TypeScript, Tailwind 4, Axios, Recharts, Radix/shadcn UI.

**Подробный ответ:** App Router (`app/`). Feature-based структура в `src/features/*`. Общий layout и UI primitives в `src/shared/`. Сборка: `next build`; CI — ESLint + build. API base URL: `NEXT_PUBLIC_API_URL` (default `http://localhost:8080/api`).

**Код:**
- `flowiq-frontend/package.json`
- `flowiq-frontend/app/layout.tsx`
- `flowiq-frontend/src/services/api.ts`

**Документация:**
- [frontend-architecture.md](frontend-architecture.md)
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) — Frontend section

**ADR:**
- [008-frontend-architecture.md](adr/008-frontend-architecture.md)

---

### Q7. Как frontend защищает маршруты без Next.js middleware?

**Краткий ответ:** **Client-side guard** в `MainLayout`: проверка `authService.isAuthenticated()` → redirect на `/login`.

**Подробный ответ:** Нет `middleware.ts` с server-side JWT validation. `MainLayout` (useEffect) читает token из `localStorage` через `authService`. Пока проверка — spinner. Страницы login/register используют `AuthLayout` без guard. Backend всё равно отклоняет запросы без валидного JWT (401).

**Код:**
- `flowiq-frontend/src/shared/components/layout/MainLayout.tsx`
- `flowiq-frontend/src/services/auth.service.ts`

**Документация:**
- [frontend-architecture.md](frontend-architecture.md)
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) — client-only auth guard

**ADR:**
- [008-frontend-architecture.md](adr/008-frontend-architecture.md)
- [006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md)

---

### Q8. Как организована feature-based структура?

**Краткий ответ:** Каждый домен — папка в `src/features/` с view, hooks, services; `app/*/page.tsx` — тонкая обёртка.

**Подробный ответ:** Пример: `app/transactions/page.tsx` рендерит `TransactionsView` из `features/transactions`. Hook `useTransactions` вызывает `transactionService`. Паттерн повторяется для analytics, forecasts, ai-accountant, tasks, notifications, imports, reports, business-guide. Dashboard — `features/dashboard/DashboardView`.

**Код:**
- `flowiq-frontend/app/page.tsx`
- `flowiq-frontend/src/features/transactions/`
- `flowiq-frontend/src/features/dashboard/`

**Документация:**
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) — Routes, Features

**ADR:**
- [008-frontend-architecture.md](adr/008-frontend-architecture.md)

---

### Q9. Какие части UI используют mock-данные вместо API?

**Краткий ответ:** **Partial mocks:** business-guide (FOP groups, KVED), tax-profile, integrations, FOP checker (client-only engine).

**Подробный ответ:** `knowledge.service.ts` ходит в `/api/business-guide` для статей. `business-guide.service.ts` — статические FOP/tax/KVED данные локально. `tax-profile.service.ts` — mock (API закомментирован). `/integrations` редиректит на coming-soon. `eligibility-engine.ts` — чисто клиентская логика без backend.

**Код:**
- `flowiq-frontend/src/features/business-guide/services/business-guide.service.ts`
- `flowiq-frontend/src/services/tax-profile.service.ts`
- `flowiq-frontend/src/services/integrations.service.ts`
- `flowiq-frontend/src/features/business-guide/checker/engine/eligibility-engine.ts`

**Документация:**
- [data-sources.md](data-sources.md)
- [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md)

**ADR:**
- [008-frontend-architecture.md](adr/008-frontend-architecture.md)

---

### Q10. Как передаются locale и currency на backend?

**Краткий ответ:** Axios interceptor добавляет заголовки **`X-App-Language`** и **`X-App-Currency`** из `localStorage`; backend читает их в `AppPreferencesFilter`.

**Подробный ответ:** `PreferencesContext` хранит language/currency/theme в `localStorage` (`flowiq_language`, `flowiq_currency`). `apiClient` interceptor на каждый запрос ставит headers. Backend `AppPreferencesFilter` создаёт thread-local `AppPreferences`, используется в `CurrencyFormatter` и AI responses. Defaults: `uk`, `UAH`.

**Код:**
- `flowiq-frontend/src/services/api.ts`
- `flowiq-frontend/src/shared/context/PreferencesContext.tsx`
- `src/main/java/com/flowiq/config/AppPreferencesFilter.java`
- `src/main/java/com/flowiq/config/AppPreferences.java`

**Документация:**
- [integration-architecture.md](integration-architecture.md)

**ADR:**
- [008-frontend-architecture.md](adr/008-frontend-architecture.md)

---

# Database (Q11–Q14)

### Q11. Какие таблицы есть в схеме?

**Краткий ответ:** **9 бизнес-таблиц:** users, transactions, chat_conversations, chat_messages, import_jobs, report_jobs, notifications, tasks, knowledge_articles.

**Подробный ответ:** Все созданы Flyway V1–V5. Tenant-scoped: transactions, chat_*, import_jobs, report_jobs, notifications, tasks (по `user_id`). Global: users, knowledge_articles. Report files хранятся как BYTEA в `report_jobs.file_content`. Enums — VARCHAR в SQL, Java enums в коде.

**Код:**
- `src/main/resources/db/migration/V1__initial_schema.sql`
- `src/main/resources/db/migration/V3__create_notifications_table.sql`
- `src/main/resources/db/migration/V4__create_tasks_table.sql`
- `src/main/resources/db/migration/V5__create_knowledge_articles_table.sql`
- `src/main/java/com/flowiq/entity/`

**Документация:**
- [database-architecture.md](database-architecture.md)
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) — Database

**ADR:**
- [004-postgresql-selection.md](adr/004-postgresql-selection.md)
- [005-flyway-selection.md](adr/005-flyway-selection.md)

---

### Q12. Какие FK enforced, а какие нет?

**Краткий ответ:** FK есть на **transactions, chat, tasks**; **нет FK** на notifications, import_jobs, report_jobs → users.

**Подробный ответ:** V1: `fk_transactions_user`, `fk_chat_*`. V4: `fk_tasks_user`. V3 notifications и V1 import/report jobs — только индекс `idx_*_user_id`, без CONSTRAINT. Риск orphan rows при удалении пользователя; компенсируется отсутствием user delete API.

**Код:**
- `src/main/resources/db/migration/V1__initial_schema.sql`
- `src/main/resources/db/migration/V3__create_notifications_table.sql`
- `src/main/resources/db/migration/V4__create_tasks_table.sql`

**Документация:**
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) — Relationships
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md)

**ADR:**
- [005-flyway-selection.md](adr/005-flyway-selection.md)

---

### Q13. Где хранятся сгенерированные отчёты?

**Краткий ответ:** В PostgreSQL — колонка **`report_jobs.file_content` (BYTEA)**; не object storage.

**Подробный ответ:** `ReportsService` генерирует PDF (OpenPDF) или XLSX (Apache POI), сохраняет bytes в `ReportJob`. Download endpoint отдаёт файл из БД. При росте объёма — риск раздувания БД и backup size (TD scalability).

**Код:**
- `src/main/java/com/flowiq/reports/entity/ReportJob.java`
- `src/main/java/com/flowiq/reports/service/ReportsService.java`
- `src/main/java/com/flowiq/reports/ReportFileGenerator.java`

**Документация:**
- [data-sources.md](data-sources.md)

**ADR:**
- [004-postgresql-selection.md](adr/004-postgresql-selection.md)

---

### Q14. Есть ли audit log в БД?

**Краткий ответ:** **Нет** — ни таблицы, ни API для аудита действий пользователя.

**Подробный ответ:** Нет записи изменений транзакций, генерации отчётов, AI/tax advice. Миграции V1–V5 не содержат `audit_log`. Зафиксировано как TD-C02 (Critical).

**Код:** *(отсутствует — проверка: нет `*Audit*` в `src/main/java`, нет migration)*

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) — TD-C02

**ADR:**
- [007-layered-architecture.md](adr/007-layered-architecture.md)

---

# AI (Q15–Q19)

### Q15. Используется ли LLM в production сейчас?

**Краткий ответ:** **Нет.** Весь «AI» — **rule-based** Java-код; в `pom.xml` нет OpenAI/Anthropic SDK.

**Подробный ответ:** `AIRecommendationEngine`, `ForecastEngine`, `RuleBasedForecastProvider`, `CategorizationEngine` + `DefaultCategoryRules`, template chat в `ChatService` / `AIAccountantService`. Интерфейсы `AIInsightProvider`, `AnalyticsInsightProvider`, `CategorizationProvider` объявлены (ADR-001), но **bean-реализаций LLM нет**.

**Код:**
- `pom.xml` — dependencies (no LLM)
- `src/main/java/com/flowiq/aiaccountant/AIRecommendationEngine.java`
- `src/main/java/com/flowiq/aiaccountant/AIInsightProvider.java`
- `src/main/java/com/flowiq/analytics/AnalyticsInsightProvider.java`

**Документация:**
- [ai-architecture.md](ai-architecture.md)
- [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md)
- [ai-agents-architecture.md](ai-agents-architecture.md)

**ADR:**
- [001-pluggable-ai-providers.md](adr/001-pluggable-ai-providers.md)
- [003-ai-quality-factory.md](adr/003-ai-quality-factory.md)

---

### Q16. Как устроен pluggable AI provider pattern?

**Краткий ответ:** Spring auto-discovery: интерфейсы + `@Component` implementations; сервисы инжектят `List<Provider>` с `@Autowired(required = false)`.

**Подробный ответ:** ADR-001: `ForecastProvider`, `KnowledgeProvider`, `AIInsightProvider`, `AnalyticsInsightProvider`, `CategorizationProvider`. Реализованы: `RuleBasedForecastProvider`, `DatabaseKnowledgeProvider`. `AIAccountantService` итерирует `insightProviders` (пустой список сейчас). Добавление LLM — новый `@Component` без изменения контроллера.

**Код:**
- `src/main/java/com/flowiq/forecast/ForecastProvider.java`
- `src/main/java/com/flowiq/forecast/RuleBasedForecastProvider.java`
- `src/main/java/com/flowiq/service/AIAccountantService.java` — constructor injection list
- `src/main/java/com/flowiq/knowledge/DatabaseKnowledgeProvider.java`

**Документация:**
- [ai-quality-factory.md](ai-quality-factory.md)

**ADR:**
- [001-pluggable-ai-providers.md](adr/001-pluggable-ai-providers.md)
- [003-ai-quality-factory.md](adr/003-ai-quality-factory.md)

---

### Q17. Чем отличается `ForecastEngine` от прогнозов в `AIAccountantService`?

**Краткий ответ:** **`ForecastEngine`** — Forecast Center (`/api/forecasts`); **`AIAccountantService.buildForecast()`** — отдельный inline расчёт для `/api/ai-accountant/forecasts`.

**Подробный ответ:** `ForecastService` делегирует в `ForecastEngine` + `RuleBasedForecastProvider`. `AIAccountantService.getForecasts()` не вызывает `ForecastEngine` — собственная логика rolling average в том же классе. Дублирование и риск расхождения метрик (technical debt).

**Код:**
- `src/main/java/com/flowiq/forecast/ForecastEngine.java`
- `src/main/java/com/flowiq/service/ForecastService.java`
- `src/main/java/com/flowiq/service/AIAccountantService.java` — `getForecasts()`, `buildForecast()`

**Документация:**
- [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md)

**ADR:**
- [001-pluggable-ai-providers.md](adr/001-pluggable-ai-providers.md)

---

### Q18. Что делает `TransactionInsightService`?

**Краткий ответ:** Собирает `FinancialSnapshot` для будущего AI; **ни один класс его не вызывает** (dead code).

**Подробный ответ:** Сервис агрегирует транзакции пользователя в контекст для insight providers. `DashboardService.getInsights()` использует inline rules, не этот сервис. `AnalyticsInsightProvider` инжектится в `AnalyticsService`, но методы провайдера не вызываются.

**Код:**
- `src/main/java/com/flowiq/service/TransactionInsightService.java`
- `src/main/java/com/flowiq/service/DashboardService.java` — inline insights
- `src/main/java/com/flowiq/service/AnalyticsService.java` — unused provider list

**Документация:**
- [AI_DOCUMENTATION_AUDIT_REPORT.md](AI_DOCUMENTATION_AUDIT_REPORT.md)
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) — dead components

**ADR:**
- [001-pluggable-ai-providers.md](adr/001-pluggable-ai-providers.md)
- [003-ai-quality-factory.md](adr/003-ai-quality-factory.md)

---

### Q19. Как работает categorization при CSV import?

**Краткий ответ:** **`CategorizationEngine`** + keyword rules (`DefaultCategoryRules`); `CategorizationProvider` (LLM) — пустой список.

**Подробный ответ:** `ImportService` парсит CSV через bank-specific strategies (`MonobankCsvStrategy`, `PrivatBankCsvStrategy`, `UniversalCsvStrategy`). Каждая строка проходит `CategorizationEngine.categorize(description)` — keyword matching. Поле `auto_categorized` (V2) помечает автоматическую категоризацию.

**Код:**
- `src/main/java/com/flowiq/import_/CategorizationEngine.java`
- `src/main/java/com/flowiq/import_/DefaultCategoryRules.java`
- `src/main/java/com/flowiq/import_/service/ImportService.java`
- `src/main/resources/db/migration/V2__add_auto_categorized_column.sql`

**Документация:**
- [data-sources.md](data-sources.md)

**ADR:**
- [001-pluggable-ai-providers.md](adr/001-pluggable-ai-providers.md)

---

# Security (Q20–Q24)

### Q20. Какая модель аутентификации?

**Краткий ответ:** **Stateless JWT** (Bearer token); сессий на сервере нет; Spring Security `STATELESS`.

**Подробный ответ:** Register/login → BCrypt hash в `users.password` → `JwtService` выдаёт access + refresh tokens. Каждый запрос: `JwtAuthenticationFilter` извлекает Bearer, валидирует access token, загружает `UserDetails`, ставит `SecurityContext`. CSRF отключён (типично для stateless API). Logout — только client-side (204 без server blacklist).

**Код:**
- `src/main/java/com/flowiq/config/SecurityConfig.java`
- `src/main/java/com/flowiq/security/JwtAuthenticationFilter.java`
- `src/main/java/com/flowiq/service/AuthService.java`

**Документация:**
- [backend-architecture.md](backend-architecture.md)

**ADR:**
- [006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md)

---

### Q21. Какие endpoints публичные?

**Краткий ответ:** Health, register, login, Swagger/OpenAPI — **без JWT**; всё остальное — `authenticated()`.

**Подробный ответ:** `SecurityConfig.requestMatchers`: `/api/health`, `/api/health/**`, `/api/auth/register`, `/api/auth/login`, `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`. `/api/auth/me`, `/logout` требуют JWT. Actuator не подключён.

**Код:**
- `src/main/java/com/flowiq/config/SecurityConfig.java` — lines 38–49

**Документация:**
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) — Security

**ADR:**
- [006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md)

---

### Q22. Как хешируются пароли?

**Краткий ответ:** **BCrypt** через `BCryptPasswordEncoder` bean в `SecurityConfig`.

**Подробный ответ:** `AuthService.register()` вызывает `passwordEncoder.encode()`. Login — `AuthenticationManager` + `DaoAuthenticationProvider` с тем же encoder. Demo user создаётся с encoded password в `DemoUserSeedService`.

**Код:**
- `src/main/java/com/flowiq/config/SecurityConfig.java` — `passwordEncoder()`
- `src/main/java/com/flowiq/service/AuthService.java`
- `src/main/java/com/flowiq/service/DemoUserSeedService.java`

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) — demo credentials risk

**ADR:**
- [006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md)

---

### Q23. Есть ли RBAC (роли) в API?

**Краткий ответ:** Поле **`User.Role`** (USER, ADMIN) в JWT claims и `UserPrincipal`, но **endpoint-level `@PreAuthorize` нет** — все authenticated users равны.

**Подробный ответ:** `JwtService` кладёт `role` в claims. `UserPrincipal.getAuthorities()` возвращает `ROLE_USER` / `ROLE_ADMIN`. `SecurityConfig` не различает роли в `authorizeHttpRequests` — только `.anyRequest().authenticated()`. Admin-only операции не реализованы.

**Код:**
- `src/main/java/com/flowiq/entity/User.java` — enum Role
- `src/main/java/com/flowiq/security/UserPrincipal.java`
- `src/main/java/com/flowiq/security/JwtService.java` — claims `role`

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md)

**ADR:**
- [006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md)

---

### Q24. Как настроен CORS?

**Краткий ответ:** Allowlist origins: **localhost:3000/3001**, Docker internal, **https://flowiq.vercel.app**; credentials enabled.

**Подробный ответ:** `CorsConfig` регистрирует `/api/**`. Allowed headers включают `Authorization`, `X-App-Language`, `X-App-Currency`. Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS. Preflight cache 3600s. Новый production origin требует изменения кода (нет env-based config).

**Код:**
- `src/main/java/com/flowiq/config/CorsConfig.java`

**Документация:**
- [integration-architecture.md](integration-architecture.md)
- [deployment/docker.md](../deployment/docker.md)

**ADR:**
- [008-frontend-architecture.md](adr/008-frontend-architecture.md)

---

# JWT (Q25–Q28)

### Q25. Какой алгоритм и библиотека JWT?

**Краткий ответ:** **JJWT** (`io.jsonwebtoken`), HMAC-SHA signing key из `jwt.secret`.

**Подробный ответ:** `JwtService` использует `Jwts.builder()` / `parser().verifyWith()`. Secret из properties кодируется Base64 перед `Keys.hmacShaKeyFor()`. Claims: `sub` (email), `type` (access|refresh), `userId`, `role`, `iat`, `exp`.

**Код:**
- `src/main/java/com/flowiq/security/JwtService.java`
- `pom.xml` — `jjwt-api`, `jjwt-impl`, `jjwt-jackson`

**Документация:**
- [backend-architecture.md](backend-architecture.md)

**ADR:**
- [006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md)

---

### Q26. Какие TTL у access и refresh token?

**Краткий ответ:** Access **24h** (86400000 ms); refresh **7d** (604800000 ms).

**Подробный ответ:** Значения в `application.properties`: `jwt.access-token-expiration`, `jwt.refresh-token-expiration`. `JwtAuthenticationFilter` принимает **только access** tokens (`isAccessToken()`). Refresh token возвращается клиенту, но серверный refresh endpoint отсутствует.

**Код:**
- `src/main/resources/application.properties` — jwt.* properties
- `src/main/java/com/flowiq/security/JwtService.java`
- `src/main/java/com/flowiq/security/JwtAuthenticationFilter.java` — line 42

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) — no `/api/auth/refresh`

**ADR:**
- [006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md)

---

### Q27. Есть ли endpoint refresh token?

**Краткий ответ:** **Нет.** `AuthController` — только register, login, me, logout.

**Подробный ответ:** `AuthService.buildAuthResponse()` генерирует refresh token и отдаёт в `AuthResponse`. Frontend сохраняет в `localStorage` (`refreshToken`), но при 401 только удаляет tokens — не вызывает refresh. Повторная аутентификация — только login.

**Код:**
- `src/main/java/com/flowiq/controller/AuthController.java`
- `src/main/java/com/flowiq/service/AuthService.java` — `buildAuthResponse()`
- `flowiq-frontend/src/services/api.ts` — 401 handler

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md)

**ADR:**
- [006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md)

---

### Q28. Где хранится JWT на клиенте?

**Краткий ответ:** **`localStorage`**: ключи `token`, `refreshToken`, `user`.

**Подробный ответ:** `authService.login/register` пишут в localStorage. `apiClient` читает `token` для `Authorization: Bearer`. Риск XSS → token theft; HttpOnly cookies не используются. При 401 (не auth endpoints) — очистка storage.

**Код:**
- `flowiq-frontend/src/services/auth.service.ts`
- `flowiq-frontend/src/services/api.ts`

**Документация:**
- [frontend-architecture.md](frontend-architecture.md)
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md)

**ADR:**
- [006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md)
- [008-frontend-architecture.md](adr/008-frontend-architecture.md)

---

# PostgreSQL (Q29–Q31)

### Q29. Почему выбран PostgreSQL?

**Краткий ответ:** ADR-004: relational model, JSON readiness, Flyway support, team familiarity, Docker image `postgres:15-alpine`.

**Подробный ответ:** ACID для financial transactions. NUMERIC(15,2) для amounts. Индексы на `(user_id)`, `(user_id, transaction_date)`. BYTEA для report blobs. Альтернативы (MySQL, Mongo) отклонены в ADR.

**Код:**
- `compose.yaml` — `postgres:15-alpine`
- `src/main/resources/application.properties` — datasource + dialect
- `src/main/resources/db/migration/V1__initial_schema.sql`

**Документация:**
- [database-architecture.md](database-architecture.md)

**ADR:**
- [004-postgresql-selection.md](adr/004-postgresql-selection.md)

---

### Q30. Как backend подключается к БД локально?

**Краткий ответ:** JDBC `jdbc:postgresql://localhost:5432/flowiq`; Docker Compose auto-start **включён** по умолчанию.

**Подробный ответ:** `application.properties`: user/password `flowiq`/`flowiq123`. `spring.docker.compose.enabled=true` поднимает `compose.yaml` при старте Spring Boot. CI отключает compose: `SPRING_DOCKER_COMPOSE_ENABLED=false`. Production должен использовать env vars / secrets manager (сейчас hardcoded в properties — debt).

**Код:**
- `src/main/resources/application.properties`
- `compose.yaml`
- `.github/workflows/backend-ci.yml` — env override

**Документация:**
- [deployment/docker.md](../deployment/docker.md)

**ADR:**
- [004-postgresql-selection.md](adr/004-postgresql-selection.md)

---

### Q31. Как JPA взаимодействует со схемой?

**Краткий ответ:** **`ddl-auto=validate`** — Hibernate не меняет схему; источник правды — **Flyway migrations**.

**Подробный ответ:** Entities мапятся на таблицы 1:1. `@ManyToOne User` на transactions. Flyway применяет SQL при старте; Hibernate только валидирует соответствие. `show-sql=true` в dev (не для prod).

**Код:**
- `src/main/resources/application.properties` — JPA + Flyway
- `src/main/java/com/flowiq/entity/Transaction.java`

**Документация:**
- [database-architecture.md](database-architecture.md)

**ADR:**
- [005-flyway-selection.md](adr/005-flyway-selection.md)
- [004-postgresql-selection.md](adr/004-postgresql-selection.md)

---

# Flyway (Q32–Q35)

### Q32. Почему Flyway, а не Hibernate ddl-auto=update?

**Краткий ответ:** ADR-005: **versioned, reviewable SQL**; reproducible deploys; `validate` в runtime.

**Подробный ответ:** Каждое изменение схемы — файл `V{n}__description.sql` в git. CI и prod применяют одинаковые миграции. Rollback — forward-only migrations (компенсирующие V{n+1}). `flyway_schema_history` — audit trail миграций.

**Код:**
- `src/main/resources/application.properties` — flyway config
- `src/main/resources/db/migration/`

**Документация:**
- [database-architecture.md](database-architecture.md)

**ADR:**
- [005-flyway-selection.md](adr/005-flyway-selection.md)

---

### Q33. Перечислите все миграции и их назначение.

**Краткий ответ:** **V1** core schema, **V2** auto_categorized, **V3** notifications, **V4** tasks, **V5** knowledge_articles + seed.

**Подробный ответ:**
- V1: users, transactions, chat, import_jobs, report_jobs + indexes/FK
- V2: `transactions.auto_categorized BOOLEAN`
- V3: notifications table + indexes (no FK)
- V4: tasks + FK to users
- V5: knowledge_articles + INSERT seed articles (Ukrainian FOP content)

**Код:**
- `src/main/resources/db/migration/V1__initial_schema.sql`
- `src/main/resources/db/migration/V2__add_auto_categorized_column.sql`
- `src/main/resources/db/migration/V3__create_notifications_table.sql`
- `src/main/resources/db/migration/V4__create_tasks_table.sql`
- `src/main/resources/db/migration/V5__create_knowledge_articles_table.sql`

**Документация:**
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) — Flyway table

**ADR:**
- [005-flyway-selection.md](adr/005-flyway-selection.md)

---

### Q34. Запускается ли Flyway в CI?

**Краткий ответ:** **Нет отдельного шага** — unit/integration тесты используют in-memory или Testcontainers (если есть); workflow не мигрирует реальный Postgres.

**Подробный ответ:** `backend-ci.yml` — только `mvnw clean verify` с `SPRING_DOCKER_COMPOSE_ENABLED=false`. Нет job «migrate staging DB». Flyway validation происходит при integration tests если поднимается контекст с H2/Testcontainers — проверить: основные тесты — unit с mocks. Gap: schema drift не ловится против prod Postgres в CI (TD).

**Код:**
- `.github/workflows/backend-ci.yml`
- `pom.xml` — test dependencies

**Документация:**
- [CI_CD_EVOLUTION_PLAN.md](../deployment/CI_CD_EVOLUTION_PLAN.md)
- [ci-cd-as-built.md](../deployment/ci-cd-as-built.md)

**ADR:**
- [005-flyway-selection.md](adr/005-flyway-selection.md)

---

### Q35. Что произойдёт при конфликте Hibernate entity и Flyway schema?

**Краткий ответ:** Старт приложения **упадёт** на `ddl-auto=validate` с `SchemaManagementException`.

**Подробный ответ:** Hibernate сравнивает entity metadata с БД. Если колонка в entity отсутствует в SQL migration — fail fast. Правильный процесс: сначала Flyway V{n}, потом обновление entity. `baseline-on-migrate=false` — чистая история с V1.

**Код:**
- `src/main/resources/application.properties` — `ddl-auto=validate`
- `src/main/resources/application.properties` — `flyway.baseline-on-migrate=false`

**Документация:**
- [database-architecture.md](database-architecture.md)

**ADR:**
- [005-flyway-selection.md](adr/005-flyway-selection.md)

---

# CI/CD (Q36–Q39)

### Q36. Что автоматизировано в CI сегодня?

**Краткий ответ:** **CI only** — backend `mvn verify` (95 tests + JaCoCo artifact); frontend lint + build. **CD нет.**

**Подробный ответ:** Два репозитория, два workflow на push/PR to `main`. Backend: Java 17 Temurin, Surefire reports → GitHub Checks, JaCoCo upload. Frontend: Node 20, `npm ci`, eslint, `next build`. Нет Docker build, deploy, E2E, security scan, Flyway against real DB.

**Код:**
- `.github/workflows/backend-ci.yml`
- `flowiq-frontend/.github/workflows/frontend-ci.yml`

**Документация:**
- [ci-cd.md](../deployment/ci-cd.md)
- [ci-cd-as-built.md](../deployment/ci-cd-as-built.md)
- [CI_CD_EVOLUTION_PLAN.md](../deployment/CI_CD_EVOLUTION_PLAN.md)

**ADR:** *(нет прямого ADR — см. deployment docs)*

---

### Q37. Сколько unit-тестов и какие исключения в Surefire?

**Краткий ответ:** **95 unit tests**; Surefire включает только `**/*Test.java` — `*Tests.java` (e.g. context load) **исключён**.

**Подробный ответ:** `maven-surefire-plugin` configuration: `<include>**/*Test.java</include>`. `FlowiqBackendApplicationTests` не попадает в CI test run. JaCoCo plugin генерирует coverage report как artifact (30 days retention). Нет enforced coverage gate в workflow.

**Код:**
- `pom.xml` — surefire + jacoco plugins
- `src/test/java/` — `*Test.java` files

**Документация:**
- [COVERAGE-REPORT.md](../COVERAGE-REPORT.md)
- [test-strategy.md](../qa/test-strategy.md)

**ADR:**
- [003-ai-quality-factory.md](adr/003-ai-quality-factory.md)

---

### Q38. Есть ли frontend тесты в CI?

**Краткий ответ:** **Нет** — только ESLint и production build; Jest/Playwright не в pipeline.

**Подробный ответ:** `frontend-ci.yml` не запускает `npm test`. TypeScript проверяется косвенно через `next build`. Нет component tests, E2E, visual regression.

**Код:**
- `flowiq-frontend/.github/workflows/frontend-ci.yml`
- `flowiq-frontend/package.json` — scripts (no test script)

**Документация:**
- [test-strategy.md](../qa/test-strategy.md)
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md)

**ADR:**
- [008-frontend-architecture.md](adr/008-frontend-architecture.md)

---

### Q39. Собирается ли Docker image в CI?

**Краткий ответ:** **Нет** — `Dockerfile` существует локально, но workflow его не вызывает.

**Подробный ответ:** `flowiq-backend/Dockerfile` — multi-stage Maven build + JRE 17 Alpine, healthcheck на `/api/health`. CI производит JAR через Maven, не публикует image в registry. CD evolution plan предлагает GHCR push на merge to main.

**Код:**
- `Dockerfile`
- `.github/workflows/backend-ci.yml`

**Документация:**
- [docker.md](../deployment/docker.md)
- [CI_CD_EVOLUTION_PLAN.md](../deployment/CI_CD_EVOLUTION_PLAN.md)

**ADR:** *(нет)*

---

# Deployment (Q40–Q43)

### Q40. Как разворачивается backend в production (as-built)?

**Краткий ответ:** **Документированный Dockerfile** + manual `docker run`; **автоматический deploy не настроен**.

**Подробный ответ:** Runtime: `eclipse-temurin:17-jre-alpine`, non-root user `flowiq`, port 8080. Env vars для datasource override. Frontend — потенциально Vercel (`flowiq.vercel.app` в CORS). Staging URL TBD (technical debt). Нет Kubernetes/Terraform в репозитории.

**Код:**
- `Dockerfile`
- `src/main/java/com/flowiq/controller/HealthController.java`

**Документация:**
- [docker.md](../deployment/docker.md)
- [ci-cd.md](../deployment/ci-cd.md)
- [CI_READINESS_REPORT.md](../deployment/CI_READINESS_REPORT.md)

**ADR:**
- [004-postgresql-selection.md](adr/004-postgresql-selection.md)

---

### Q41. Что поднимает `compose.yaml`?

**Краткий ответ:** Только **PostgreSQL 15** — не full stack (backend/frontend не в compose).

**Подробный ответ:** Service `postgres`: port 5432, volume `postgres-data`, healthcheck `pg_isready`. Backend и frontend запускаются отдельно (IDE / npm / docker run). Spring Boot Docker Compose integration auto-starts этот файл при dev.

**Код:**
- `compose.yaml`

**Документация:**
- [docker.md](../deployment/docker.md)

**ADR:**
- [004-postgresql-selection.md](adr/004-postgresql-selection.md)

---

### Q42. Какой health endpoint для load balancer?

**Краткий ответ:** **`GET /api/health`** (public) — status, app name, timestamp; Docker HEALTHCHECK использует его.

**Подробный ответ:** `HealthController` — не Spring Actuator. `/api/health/ping` — lightweight ping. Нет `/actuator/health`, нет readiness probe для PostgreSQL dependency в health response.

**Код:**
- `src/main/java/com/flowiq/controller/HealthController.java`
- `Dockerfile` — HEALTHCHECK curl

**Документация:**
- [monitoring.md](../operations/monitoring.md)

**ADR:** *(нет)*

---

### Q43. Какие secrets опасны в default config?

**Краткий ответ:** **`jwt.secret`**, **DB password `flowiq123`**, **demo user `demo@flowiq.ai` / `demo123`** в коде.

**Подробный ответ:** `application.properties` содержит dev JWT secret (должен быть env в prod). `DemoUserSeedService` создаёт пользователя с известным паролем на каждом новом окружении. `DemoUserSeedService` логирует credentials в INFO. Требуется external secrets + profile-gated seed.

**Код:**
- `src/main/resources/application.properties`
- `src/main/java/com/flowiq/service/DemoUserSeedService.java`

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) — TD-C03, TD-H*

**ADR:**
- [006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md)
- [002-transaction-seed-strategy.md](adr/002-transaction-seed-strategy.md)

---

# Scalability (Q44–Q46)

### Q44. Можно ли горизонтально масштабировать backend?

**Краткий ответ:** **Частично** — stateless API позволяет несколько инстансов, но **schedulers и seed** создают проблемы.

**Подробный ответ:** JWT stateless — OK за load balancer. `@Scheduled` jobs (`DailyTaskScheduler`, `NotificationScheduler`) выполнятся на **каждом** инстансе без leader election → duplicate tasks/notifications. `TransactionSeedService` и `DemoUserSeedService` — race-safe частично (exists checks), но не для distributed cache. Sticky sessions не нужны.

**Код:**
- `src/main/java/com/flowiq/tasks/scheduler/DailyTaskScheduler.java`
- `src/main/java/com/flowiq/notifications/scheduler/NotificationScheduler.java`
- `src/main/java/com/flowiq/config/SecurityConfig.java` — STATELESS

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md)
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) — Schedulers

**ADR:**
- [007-layered-architecture.md](adr/007-layered-architecture.md)

---

### Q45. Где узкие места при росте данных?

**Краткий ответ:** **BYTEA reports в Postgres**, full table scans в schedulers (`userRepository.findAll()`), агрегации transactions без partitioning.

**Подробный ответ:** Каждый daily scheduler загружает всех active users в память. Transaction queries фильтруют по `user_id` + date — индексы есть, но нет archival/partitioning. Report PDFs в БД увеличивают backup size. Chat messages — TEXT без лимита retention policy.

**Код:**
- `src/main/java/com/flowiq/notifications/scheduler/NotificationScheduler.java` — `findAll()`
- `src/main/java/com/flowiq/reports/entity/ReportJob.java`
- `src/main/resources/db/migration/V1__initial_schema.sql` — indexes

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md)

**ADR:**
- [004-postgresql-selection.md](adr/004-postgresql-selection.md)

---

### Q46. Есть ли кэширование?

**Краткий ответ:** **Нет** application-level cache (Redis, Caffeine, `@Cacheable`).

**Подробный ответ:** Каждый dashboard/analytics request читает DB. Frontend не использует React Query/SWR — hooks делают fetch on mount. CORS preflight cache 3600s — единственный «кэш». Для scale — добавить read-through cache per user dashboard snapshot.

**Код:** *(grep: no `@EnableCaching`, no redis dependency in `pom.xml`)*

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md)

**ADR:**
- [007-layered-architecture.md](adr/007-layered-architecture.md)

---

# Performance (Q47–Q49)

### Q47. Какие тяжёлые операции синхронные?

**Краткий ответ:** **CSV import**, **report generation** (PDF/XLSX), **aggregations** в analytics/forecast — всё в HTTP request thread.

**Подробный ответ:** `ImportService` парсит файл до 10MB (`spring.servlet.multipart.max-*`). `ReportsService` генерирует document synchronously, сохраняет в DB, возвращает job id. Нет `@Async`, нет message queue. Long-running reports могут timeout на reverse proxy.

**Код:**
- `src/main/resources/application.properties` — multipart 10MB
- `src/main/java/com/flowiq/import_/service/ImportService.java`
- `src/main/java/com/flowiq/reports/service/ReportsService.java`

**Документация:**
- [backend-architecture.md](backend-architecture.md)

**ADR:**
- [007-layered-architecture.md](adr/007-layered-architecture.md)

---

### Q48. Есть ли пагинация в API?

**Краткий ответ:** **Да** для transactions (`Pageable`); многие list endpoints возвращают **полные списки** без page.

**Подробный ответ:** `TransactionController` / `TransactionService` — Spring Data `Page<Transaction>`. Notifications, tasks, chat conversations — list endpoints без universal pagination pattern. При большом inbox — payload growth.

**Код:**
- `src/main/java/com/flowiq/service/TransactionService.java`
- `src/main/java/com/flowiq/controller/TransactionController.java`
- `src/main/java/com/flowiq/notifications/service/NotificationService.java`

**Документация:**
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md)

**ADR:**
- [007-layered-architecture.md](adr/007-layered-architecture.md)

---

### Q49. Как frontend оптимизирует рендеринг?

**Краткий ответ:** **Client components** (`"use client"`), Framer Motion анимации, Recharts для графиков; **нет** virtualized lists, SSR data fetching minimal.

**Подробный ответ:** App Router pages mostly client-side data fetch after mount. `next build` static optimization где возможно. Dashboard загружает несколько parallel API calls. Bundle includes Recharts + framer-motion — следить за size budget.

**Код:**
- `flowiq-frontend/src/features/dashboard/DashboardView.tsx`
- `flowiq-frontend/app/page.tsx`

**Документация:**
- [frontend-architecture.md](frontend-architecture.md)

**ADR:**
- [008-frontend-architecture.md](adr/008-frontend-architecture.md)

---

# Monitoring (Q50–Q52)

### Q50. Есть ли Spring Boot Actuator?

**Краткий ответ:** **Нет** — `spring-boot-starter-actuator` отсутствует в `pom.xml`.

**Подробный ответ:** Мониторинг ограничен custom `HealthController`. Нет `/actuator/prometheus`, metrics, thread dumps. `docs/operations/monitoring.md` — checklist рекомендаций, не as-built. TD-H08 в debt register.

**Код:**
- `pom.xml` — no actuator dependency
- `src/main/java/com/flowiq/controller/HealthController.java`

**Документация:**
- [monitoring.md](../operations/monitoring.md)
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) — TD-H08

**ADR:** *(нет)*

---

### Q51. Есть ли structured logging и correlation IDs?

**Краткий ответ:** **Нет** request correlation ID filter; стандартный SLF4J + log levels в properties.

**Подробный ответ:** Schedulers логируют INFO/WARN. `JwtAuthenticationFilter` глотает exceptions silently (`catch (Exception ignored)`). Нет JSON log format, нет traceId в MDC. Distributed tracing (OpenTelemetry) не подключён.

**Код:**
- `src/main/java/com/flowiq/security/JwtAuthenticationFilter.java` — line 60
- `src/main/java/com/flowiq/notifications/scheduler/NotificationScheduler.java` — logging

**Документация:**
- [monitoring.md](../operations/monitoring.md)
- [CI_CD_EVOLUTION_PLAN.md](../deployment/CI_CD_EVOLUTION_PLAN.md) — observability phase

**ADR:** *(нет)*

---

### Q52. Есть ли error tracking на frontend?

**Краткий ответ:** **Нет** Sentry/LogRocket; ошибки API только `Promise.reject` в axios interceptor.

**Подробный ответ:** 401 обрабатывается (clear tokens). Другие 4xx/5xx — на усмотрение feature hooks (обычно local error state). Нет global error boundary reporting. Рекомендация в monitoring.md — добавить Sentry.

**Код:**
- `flowiq-frontend/src/services/api.ts`
- `flowiq-frontend/package.json` — no sentry dependency

**Документация:**
- [monitoring.md](../operations/monitoring.md)
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md)

**ADR:**
- [008-frontend-architecture.md](adr/008-frontend-architecture.md)

---

# Technical Debt (Q53–Q56)

### Q53. Топ-3 critical debt items?

**Краткий ответ:** **TD-C01** auto-seed transactions, **TD-C02** no audit log, **TD-C03** demo user in all envs.

**Подробный ответ:** Auto-seed искажает tax/FOP метрики для новых пользователей. Нет audit trail для compliance. Demo credentials в коде + логирование пароля. Все три блокируют «trustworthy production launch» по internal audit.

**Код:**
- `src/main/java/com/flowiq/service/TransactionSeedService.java`
- `src/main/java/com/flowiq/service/DemoUserSeedService.java`

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md) — Critical section

**ADR:**
- [002-transaction-seed-strategy.md](adr/002-transaction-seed-strategy.md)

---

### Q54. Сколько debt items и roadmap?

**Краткий ответ:** **48 items** (6 Critical, 14 High, 16 Medium, 12 Low); **3-month roadmap** в конце register.

**Подробный ответ:** Roadmap Month 1: production safety (disable seed, secrets, refresh token). Month 2: CI hardening (Postgres in CI, Docker publish). Month 3: observability + settings API. Exit criteria включают `/actuator/health` и staging URL.

**Код:** *(meta-document — implementation scattered)*

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md)

**ADR:**
- [ADR_COVERAGE_REPORT.md](adr/ADR_COVERAGE_REPORT.md)

---

### Q55. Дублирование FOP/tax констант?

**Краткий ответ:** **Да** — лимиты доходов, ЕСВ, пороги дублируются в backend engines и frontend mock services.

**Подробный ответ:** `NotificationRuleEngine`, `AIRecommendationEngine`, `TaskRuleEngine` содержат hardcoded FOP thresholds. Frontend `business-guide.service.ts` / `eligibility-engine.ts` — свои копии. Риск расхождения при изменении законодательства (TD-H* в register).

**Код:**
- `src/main/java/com/flowiq/notifications/service/NotificationRuleEngine.java` — ESV, INCOME_LIMITS
- `src/main/java/com/flowiq/aiaccountant/AIRecommendationEngine.java`
- `flowiq-frontend/src/features/business-guide/checker/engine/eligibility-engine.ts`

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md)
- [data-sources.md](data-sources.md)

**ADR:**
- [001-pluggable-ai-providers.md](adr/001-pluggable-ai-providers.md)

---

### Q56. Что не реализовано, но задокументировано в API?

**Краткий ответ:** **Refresh token endpoint**, **bank integrations** (`flowiq.features.bank-integrations-enabled=false`), **settings persistence API**, **email/Telegram notifications**.

**Подробный ответ:** `FeatureFlags` — integrations off. `NotificationChannel` enum включает EMAIL/TELEGRAM, но generator пишет только IN_APP. Frontend settings — localStorage only, нет `PUT /api/users/preferences`. Refresh token выдаётся, но не consumable.

**Код:**
- `src/main/java/com/flowiq/config/FeatureFlags.java`
- `src/main/resources/application.properties` — `flowiq.features.bank-integrations-enabled=false`
- `flowiq-frontend/app/settings/page.tsx`

**Документация:**
- [TECHNICAL_DEBT_REGISTER.md](TECHNICAL_DEBT_REGISTER.md)
- [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md)

**ADR:**
- [001-pluggable-ai-providers.md](adr/001-pluggable-ai-providers.md)
- [008-frontend-architecture.md](adr/008-frontend-architecture.md)

---

## Quick reference — ADR index

| ADR | Topic | File |
|-----|-------|------|
| 001 | Pluggable AI providers | [adr/001-pluggable-ai-providers.md](adr/001-pluggable-ai-providers.md) |
| 002 | Transaction seed strategy | [adr/002-transaction-seed-strategy.md](adr/002-transaction-seed-strategy.md) |
| 003 | AI quality factory | [adr/003-ai-quality-factory.md](adr/003-ai-quality-factory.md) |
| 004 | PostgreSQL | [adr/004-postgresql-selection.md](adr/004-postgresql-selection.md) |
| 005 | Flyway | [adr/005-flyway-selection.md](adr/005-flyway-selection.md) |
| 006 | JWT auth | [adr/006-jwt-authentication-strategy.md](adr/006-jwt-authentication-strategy.md) |
| 007 | Layered architecture | [adr/007-layered-architecture.md](adr/007-layered-architecture.md) |
| 008 | Frontend architecture | [adr/008-frontend-architecture.md](adr/008-frontend-architecture.md) |

---

## Related documents

| Document | Purpose |
|----------|---------|
| [ARCHITECT_REVIEW_CHEAT_SHEET.md](ARCHITECT_REVIEW_CHEAT_SHEET.md) | 15-min presentation script |
| [ADR_DEFENSE_GUIDE.md](ADR_DEFENSE_GUIDE.md) | Deep ADR Q&A |
| [SYSTEM_COMPONENT_CATALOG.md](SYSTEM_COMPONENT_CATALOG.md) | Full component inventory |
| [ARCHITECTURE_REVIEW_READINESS.md](ARCHITECTURE_REVIEW_READINESS.md) | Review checklist |

**Total questions:** 56  
**Last verified against code:** 2026-06-23
