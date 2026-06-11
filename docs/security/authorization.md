# Security: Authorization

## Role Model

`User.role` enum: `ADMIN`, `USER`, `VIEWER`

## Current Enforcement

- **Authentication required** for all non-public API paths
- **No method-level `@PreAuthorize`** on controllers today
- All data scoped by authenticated user ID in services

## Data Isolation

Services resolve current user via `SecurityContextHolder` → filter transactions/tasks/notifications by `userId`.

**Gap:** No explicit authorization tests; role `VIEWER` not enforced differently from `USER`.

## Future

- [ ] `@PreAuthorize("hasRole('ADMIN')")` for admin endpoints
- [ ] Accountant multi-client access model
- [ ] Resource-level ownership checks on every `/{id}` endpoint

## Related

- [Authentication](authentication.md)
