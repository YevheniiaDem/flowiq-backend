# Critical User Flows

## 1. Login → Dashboard

```mermaid
flowchart LR
    A[/login] --> B[Enter credentials]
    B --> C[POST /auth/login]
    C --> D[Store JWT]
    D --> E[/ Dashboard]
    E --> F[Parallel API load]
```

**Verify:** Stats render, no 401, demo data visible.

## 2. Create Transaction

1. Navigate `/transactions`
2. Click Add → fill amount, category, date
3. Save → `POST /api/transactions`
4. Row appears in table; dashboard stats update on refresh

## 3. Import Bank CSV

1. `/imports` → upload Monobank/PrivatBank CSV
2. Job status COMPLETED
3. Transactions visible; optional review task created
4. Notification received

## 4. View Forecast

1. `/forecasts` or dashboard widget
2. Charts load from `/api/forecasts/*`
3. Insights and warnings displayed
4. FOP limit card shows usage %

## 5. Complete Task

1. `/tasks` → select overdue/today task
2. Mark complete → `PUT /api/tasks/{id}/complete`
3. Moves to completed group; snapshot updates

## 6. Search Knowledge Base

1. `/business-guide` → search "ФОП 3 група податки"
2. AI summary appears in dropdown
3. Click article → `/business-guide/articles/{slug}`
4. Related articles shown

## 7. Generate Report

1. `/reports` → select type, period, PDF
2. Generate → poll job status
3. Download file opens valid PDF

## 8. Language Switch

1. `/settings` → language EN
2. Business Guide articles return English titles
3. UI labels update via `t()`
