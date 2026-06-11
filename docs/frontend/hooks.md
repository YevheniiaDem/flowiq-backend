# Frontend Hooks

| Hook | File | Purpose |
|------|------|---------|
| `usePreferences` | `shared/context/PreferencesContext.tsx` | i18n, currency |
| `useTasks` | `features/tasks/hooks/useTasks.ts` | Task CRUD, filters |
| `useForecasts` | `features/forecasts/hooks/useForecasts.ts` | Forecast data |
| `useTransactions` | `features/transactions/hooks/useTransactions.ts` | Transaction list |
| `useAnalytics` | `features/analytics/hooks/useAnalytics.ts` | Analytics charts |
| `useAIAccountant` | `features/ai-accountant/hooks/useAIAccountant.ts` | AI accountant data |
| `useReports` | `features/reports/hooks/useReports.ts` | Report jobs |
| `useImports` | `features/imports/hooks/useImports.ts` | Import history |
| `useNotifications` | `features/notifications/hooks/useNotifications.ts` | Notification feed |
| `useUnreadCount` | same | Bell badge count |
| `useBusinessGuide` | `features/business-guide/hooks/useBusinessGuide.ts` | Profile, groups mock |
| `useBusinessGuideSearch` | `hooks/useBusinessGuideSearch.ts` | Knowledge search API |
| `useKnowledgeArticles` | `hooks/useKnowledgeArticles.ts` | Article lists |
| `useKnowledgeArticle` | `hooks/useKnowledgeArticle.ts` | Single article |
| `useFopGroup` | `hooks/useFopGroup.ts` | Group detail mock |
| `useKvedSearch` | `hooks/useKvedSearch.ts` | KVED filter mock |
| `useEligibilityChecker` | `checker/hooks/useEligibilityChecker.ts` | FOP checker flow |

## Debouncing

`useBusinessGuideSearch` — 300ms debounce before API call.

## Related

- [State Management](state-management.md)
