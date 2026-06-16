# Frontend Routing

**Router:** Next.js 16 App Router  
**No middleware.ts** — auth via `MainLayout` client guard

## Route Tree

```
app/
├── layout.tsx          # Root + Providers
├── page.tsx            # /
├── login/page.tsx
├── register/page.tsx
├── transactions/page.tsx
├── imports/page.tsx
├── analytics/page.tsx
├── chat/page.tsx
├── ai-accountant/page.tsx
├── forecasts/page.tsx
├── tasks/page.tsx
├── reports/page.tsx
├── notifications/page.tsx
├── coming-soon/
│   └── integrations/page.tsx   # Hidden — bank integrations planned
├── integrations/page.tsx       # Redirects to coming-soon
├── settings/page.tsx
└── business-guide/
    ├── page.tsx
    ├── articles/[slug]/page.tsx
    └── groups/[slug]/page.tsx
```

## Layouts

| Layout | Used By |
|--------|---------|
| `AuthLayout` | login, register |
| `MainLayout` | all authenticated pages (Sidebar + TopNav) |

## SEO-Friendly Routes

- Articles: `/business-guide/articles/fop-group-3-taxes-faq`
- Query tabs: `/business-guide?tab=updates`
- Group detail: `/business-guide/groups/2`

## Deep Links (from notifications)

Match `Notification.action_url` paths in sidebar navigation.

## Related

- [Pages](pages.md)
