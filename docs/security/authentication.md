# Security: Authentication

## Mechanism

**JWT Bearer tokens** with stateless sessions.

## Components

| Component | File |
|-----------|------|
| `SecurityConfig` | `config/SecurityConfig.java` |
| `JwtService` | `security/JwtService.java` |
| `JwtAuthenticationFilter` | `security/JwtAuthenticationFilter.java` |
| `CustomUserDetailsService` | `security/CustomUserDetailsService.java` |
| `UserPrincipal` | `security/UserPrincipal.java` |

## Password Policy

Validated in `RegisterRequest` / `LoginRequest` (email format, password not blank). Stored as BCrypt hash.

## Public Endpoints

- `/api/health/**`
- `/api/auth/register`, `/api/auth/login`
- Swagger UI / OpenAPI docs

## Frontend

Token in `localStorage` — **XSS risk**; consider httpOnly cookies for production hardening.

## Related

- [JWT Flow](jwt-flow.md)
- [Authorization](authorization.md)
