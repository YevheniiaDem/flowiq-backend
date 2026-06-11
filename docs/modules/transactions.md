# Transactions Module

**Controller:** `TransactionController`  
**Service:** `TransactionService`  
**Frontend:** `features/transactions/`

## Features

- CRUD with validation (`CreateTransactionRequest`, `UpdateTransactionRequest`)
- Pagination, search, type/date filters
- Summary endpoint for dashboard cards
- CSV import via `ImportService` → auto-categorization

## Transaction Types

`REVENUE` | `EXPENSE`

## Import Pipeline

See [Integration Architecture](../architecture/integration-architecture.md).

## Auto-Categorization

`CategorizationEngine` applies `DefaultCategoryRules` on import; sets `auto_categorized=true`.

## API

Implicit in OpenAPI — `GET/POST/PUT/DELETE /api/transactions`.

## Frontend Hooks

`useTransactions` — list, filter, modal CRUD.
