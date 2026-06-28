# Authentication Flow

**As-built:** 2026-06-28  
**Backend:** `AuthService`, `JwtService`, `SessionService`, `JwtAuthenticationFilter`  
**Frontend:** `authService`, `tokenRefresh.ts`, `apiClient`

## Overview

FlowIQ uses **stateless JWT authentication** with **server-side refresh token sessions**. Access tokens authorize API calls; refresh tokens rotate on each refresh and are stored hashed in `user_sessions`.

## Token Types

| Token | Claim `type` | Lifetime (dev) | Used for |
|-------|--------------|----------------|----------|
| Access | `access` | 24 h | `Authorization: Bearer` on all protected `/api/*` |
| Refresh | `refresh` | 7 d | `POST /api/auth/refresh` only |

## Login & Registration Sequence

```mermaid
sequenceDiagram
    participant UI as Frontend
    participant AC as AuthController
    participant AS as AuthService
    participant AM as AuthenticationManager
    participant JS as JwtService
    participant SS as SessionService
    participant FPS as FopProfileService
    participant UR as UserRepository
    participant DB as PostgreSQL
    participant AUD as AuditService

    UI->>AC: POST /api/auth/register or /login
    alt Register
        AC->>AS: register(request)
        AS->>UR: save User (BCrypt password)
        AS->>FPS: getOrCreateForUser(user)
        AS->>AUD: AUTH_REGISTER
    else Login
        AC->>AS: login(request)
        AS->>AM: authenticate(email, password)
        AS->>AUD: AUTH_LOGIN
    end
    AS->>JS: generateAccessToken(principal)
    AS->>JS: generateRefreshToken(principal)
    AS->>SS: createSession(user, refreshToken, userAgent, ip)
    SS->>DB: INSERT user_sessions (hashed refresh)
    AS-->>UI: AuthResponse { token, refreshToken, user }
    UI->>UI: localStorage.setItem(access + refresh)
```

## Protected Request Sequence

```mermaid
sequenceDiagram
    participant UI as Frontend apiClient
    participant F as JwtAuthenticationFilter
    participant JS as JwtService
    participant UDS as CustomUserDetailsService
    participant C as Controller
    participant S as Service

    UI->>F: GET/POST /api/* + Bearer accessToken
    F->>JS: extractUsername + isTokenValid
    F->>JS: isAccessToken (reject refresh tokens)
    alt Invalid or refresh token
        F-->>UI: 401 Unauthorized
    else Valid access token
        F->>UDS: loadUserByUsername(email)
        F->>F: SecurityContextHolder.setAuthentication
        F->>C: chain.doFilter
        C->>S: business logic (user from SecurityContext)
        S-->>UI: 200 JSON
    end
```

## Token Refresh Sequence

```mermaid
sequenceDiagram
    participant UI as tokenRefresh.ts
    participant AC as AuthController
    participant AS as AuthService
    participant JS as JwtService
    participant SS as SessionService
    participant DB as PostgreSQL

    UI->>AC: POST /api/auth/refresh { refreshToken }
    AC->>AS: refresh(request)
    AS->>JS: extractUsername + isTokenValid
    AS->>JS: isRefreshToken
    AS->>SS: validateSession(userId, refreshToken)
    SS->>DB: SELECT user_sessions WHERE hash matches
    alt Session invalid or revoked
        AS-->>UI: 401 Unauthorized
    else Valid session
        AS->>JS: generateAccessToken + generateRefreshToken
        AS->>SS: rotateSession (new hash)
        SS->>DB: UPDATE user_sessions
        AS-->>UI: RefreshTokenResponse { token, refreshToken }
        UI->>UI: Update localStorage
    end
```

Frontend interceptor (`apiClient`) retries failed requests once after successful refresh on HTTP 401.

## Logout Sequence

```mermaid
sequenceDiagram
    participant UI as Frontend
    participant AC as AuthController
    participant AS as AuthService
    participant SS as SessionService

    UI->>AC: POST /api/auth/logout (Bearer access)
    AC->>AS: logout()
    AS->>SS: revokeCurrentSession()
    AS-->>UI: 204
    UI->>UI: Clear localStorage tokens
```

Profile API also supports `POST /api/profile/sessions/logout-all` to revoke all sessions.

## Public vs Protected Routes

| Path pattern | Auth |
|--------------|------|
| `POST /api/auth/register`, `/login`, `/refresh` | Public |
| `GET /api/health`, `/api/health/ping` | Public |
| `GET /api/profile/avatars/{filename}` | Public |
| `/swagger-ui/**`, `/v3/api-docs/**` | Public |
| All other `/api/**` | JWT access token required |

Configured in `SecurityConfig`.

## Frontend Auth Guard

```mermaid
flowchart TD
    A[Route change] --> B{MainLayout}
    B --> C{authService.isAuthenticated?}
    C -->|No| D[Redirect /login]
    C -->|Yes| E[Render feature view]
    E --> F{API 401?}
    F -->|Yes| G[tokenRefresh.refreshToken]
    G -->|Success| H[Retry request]
    G -->|Fail| D
```

**Note:** No Next.js middleware — guard is client-side only in `MainLayout`.

## Security Considerations

| Topic | Implementation |
|-------|----------------|
| Password storage | BCrypt via `PasswordEncoder` |
| Refresh token storage (server) | SHA-256 hash in `user_sessions.refresh_token_hash` |
| Refresh token storage (client) | `localStorage` — XSS surface |
| Role enforcement | Roles in JWT; **no `@PreAuthorize`** today |
| Audit | `AUTH_*` events via `AuditService` |

## Related

- [ADR-006: JWT Authentication](adr/006-jwt-authentication-strategy.md)
- [Security: JWT Flow](../security/jwt-flow.md) — quick reference
- [Profile Architecture](PROFILE_ARCHITECTURE.md) — session management UI
- [SRS §6](../product/SRS.md) — security requirements
