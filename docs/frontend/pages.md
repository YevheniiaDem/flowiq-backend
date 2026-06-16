# Frontend Pages

| Route | Page File | View Component | API Dependencies |
|-------|-----------|----------------|------------------|
| `/` | `app/page.tsx` | `DashboardView` | dashboard, tax profile (mock), forecast/tasks/guide snapshots |
| `/login` | `app/login/page.tsx` | `LoginForm` | `POST /auth/login` |
| `/register` | `app/register/page.tsx` | `RegisterForm` | `POST /auth/register` |
| `/transactions` | `app/transactions/page.tsx` | `TransactionsView` | `/transactions` |
| `/imports` | `app/imports/page.tsx` | `ImportsView` | `/imports` |
| `/analytics` | `app/analytics/page.tsx` | `AnalyticsView` | `/analytics/*` |
| `/chat` | `app/chat/page.tsx` | `ChatView` | `/chat/*` |
| `/ai-accountant` | `app/ai-accountant/page.tsx` | `AIAccountantView` | `/ai-accountant/*` |
| `/forecasts` | `app/forecasts/page.tsx` | `ForecastsView` | `/forecasts/*` |
| `/tasks` | `app/tasks/page.tsx` | `TasksView` | `/tasks/*` |
| `/reports` | `app/reports/page.tsx` | `ReportsView` | `/reports/*` |
| `/notifications` | `app/notifications/page.tsx` | `NotificationCenterView` | `/notifications` |
| `/coming-soon/integrations` | `app/coming-soon/integrations/page.tsx` | `IntegrationsComingSoonView` | none (hidden) |
| `/integrations` | `app/integrations/page.tsx` | redirect → coming-soon | — |
| `/business-guide` | `app/business-guide/page.tsx` | `BusinessGuideView` | knowledge API + local mock |
| `/business-guide/articles/[slug]` | `app/business-guide/articles/[slug]/page.tsx` | `KnowledgeArticleView` | `/business-guide/articles/{slug}` |
| `/business-guide/groups/[slug]` | `app/business-guide/groups/[slug]/page.tsx` | `GroupDetailView` | local mock |
| `/settings` | `app/settings/page.tsx` | `SettingsView` | preferences only |

## User Actions per Page

### Dashboard `/`
- View stats, charts, insights
- Navigate to modules via widgets
- Open Business Guide / run eligibility check

### Transactions `/transactions`
- Create, edit, delete transactions
- Filter by date, type, search
- View summary cards

### Forecasts `/forecasts`
- View 6-month projections
- Read insights and warning banners

### Tasks `/tasks`
- CRUD tasks, complete, calendar view
- Filter by type/priority/status

### Business Guide `/business-guide`
- Tab navigation (7 tabs)
- Global smart search with AI summary
- Read articles, legal updates

## Related

- [Routing](routing.md)
- [Components](components.md)
