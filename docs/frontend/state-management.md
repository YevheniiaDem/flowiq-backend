# State Management

## Approach

**No Redux / Zustand / React Query.** Local component state + React Context.

## Global Context

### PreferencesContext

**File:** `src/shared/context/PreferencesContext.tsx`

| State | Storage | Default |
|-------|---------|---------|
| `language` | localStorage `flowiq_language` | `uk` |
| `currency` | localStorage `flowiq_currency` | `UAH` |

Provides `t(key, params)` for i18n.

## Auth State

Stored in `localStorage`:
- `token` — JWT access
- `refreshToken` — not actively used
- `user` — JSON serialized

Managed by `auth.service.ts`, read by `apiClient` interceptor and `TopNav`.

## Feature Data Pattern

```typescript
const [data, setData] = useState(null);
const [loading, setLoading] = useState(true);
const [error, setError] = useState(null);

useEffect(() => {
  service.getData().then(setData).catch(setError).finally(() => setLoading(false));
}, [deps]);
```

Encapsulated in feature hooks (`useTasks`, `useForecasts`, etc.).

## API Client

Single Axios instance — no per-request cache invalidation layer.

## Related

- [Hooks](hooks.md)
