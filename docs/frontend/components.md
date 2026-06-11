# Frontend Components

## Shared Layout (`src/shared/components/`)

| Component | Path | Purpose |
|-----------|------|---------|
| `MainLayout` | `layout/MainLayout.tsx` | Auth guard, shell |
| `Sidebar` | `layout/Sidebar.tsx` | Navigation |
| `TopNav` | `layout/TopNav.tsx` | User menu, notifications bell |
| `Providers` | `Providers.tsx` | PreferencesContext wrapper |

## UI Primitives (`src/shared/components/ui/`)

shadcn-style: `Button`, `Card`, `Badge`, `Dialog`, `Input`, `Tabs`, `Sheet`, `Avatar`, `ClearableInput`, `DropdownMenu`, `Tooltip`

## Feature Components (by module)

See exploration report — each `src/features/{name}/components/` contains view + subcomponents.

### Notable Cross-Feature Widgets

| Component | Feature | Used On |
|-----------|---------|---------|
| `ForecastSnapshotWidget` | dashboard | Dashboard |
| `TasksDashboardWidget` | tasks | Dashboard |
| `BusinessGuideDashboardWidget` | business-guide | Dashboard |
| `RecentNotificationsWidget` | notifications | Dashboard |
| `NotificationBell` | notifications | TopNav |

## Business Guide Component Tree

```
BusinessGuideView
├── BusinessGuideHero (search)
├── BusinessGuideTabs
├── BusinessProfileCard
├── FopGroupsOverview → FopGroupCard
├── FopEligibilityChecker
├── TaxesSection → TaxCard
├── KvedExplorer
├── KnowledgeArticleList → KnowledgeArticleCard
├── LegalUpdatesSection
└── BusinessGuideDashboardWidget
```

## Related

- [Hooks](hooks.md)
- [State Management](state-management.md)
