# Frontend Architecture

**Repository:** `flowiq-frontend`  
**Framework:** Next.js 16 App Router, React 19, TypeScript

## Structure

```
flowiq-frontend/
├── app/                    # Routes (thin pages)
├── src/
│   ├── features/           # Domain modules (14)
│   ├── services/           # Shared API clients
│   ├── shared/
│   │   ├── components/     # layout, ui
│   │   ├── context/        # PreferencesContext
│   │   ├── i18n/           # uk/en
│   │   └── utils/
│   └── mock-data/          # tax profile (pending API)
```

## Feature Module Pattern

Each feature typically contains:

| Folder | Contents |
|--------|----------|
| `components/` | View + presentational components |
| `hooks/` | Data fetching (`useState` + `useEffect`) |
| `services/` | Axios calls via `apiClient` |
| `types/` | TypeScript interfaces |
| `index.ts` | Public exports |

## Layout & Auth Guard

```mermaid
flowchart TB
    ROOT[app/layout.tsx Providers]
    AUTH[AuthLayout /login /register]
    MAIN[MainLayout protected routes]

    ROOT --> AUTH
    ROOT --> MAIN
    MAIN -->|!token| LOGIN[/login redirect]
    MAIN --> SIDEBAR[Sidebar]
    MAIN --> TOP[TopNav]
    MAIN --> PAGE[Feature View]
```

`MainLayout` (`src/shared/components/layout/MainLayout.tsx`):
- Checks `authService.isAuthenticated()` (localStorage token)
- Redirects unauthenticated users to `/login`
- **No Next.js middleware** — client-side guard only

## API Integration

`src/services/api.ts`:
- Base URL: `NEXT_PUBLIC_API_URL` || `http://localhost:8080/api`
- Injects `Authorization: Bearer`, `X-App-Language`, `X-App-Currency`
- 401 clears token (except auth endpoints)

## Styling Conventions

- Tailwind CSS 4 + design tokens (`primary`, `muted-foreground`, `border-border`)
- shadcn/ui primitives (`Card`, `Button`, `Badge`, `Dialog`)
- framer-motion for entrance animations
- Glassmorphism: `bg-card/50 backdrop-blur-sm`

## i18n

Custom implementation (no next-intl):
- `PreferencesContext` → `t("namespace.key")`
- Locales: `src/shared/i18n/locales/uk.ts`, `en.ts`
- Type-safe `TranslationKey` union in `index.ts`

## Data Sources

| Feature | Source |
|---------|--------|
| Transactions, Forecasts, Tasks, Notifications, Knowledge | Backend API |
| Tax profile card | `mock-data/tax-profile.localized.ts` |
| Business Guide groups/taxes (partial) | `features/business-guide/data/` |
| FOP Eligibility Checker | Client-side `eligibility-engine.ts` |
| Integrations | Stub service |

## Related Documents

- [Pages](../frontend/pages.md)
- [Routing](../frontend/routing.md)
- [Components](../frontend/components.md)
- [State Management](../frontend/state-management.md)
- [Hooks](../frontend/hooks.md)
