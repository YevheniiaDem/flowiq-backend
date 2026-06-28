# JWT Flow

> **Canonical documentation:** [architecture/flows/authentication-flow.md](../architecture/flows/authentication-flow.md)

Quick reference for JWT access + refresh token flow.

```mermaid
sequenceDiagram
    participant C as Client
    participant A as AuthController
    participant J as JwtService
    participant SS as SessionService
    participant F as JwtAuthenticationFilter
    participant API as Protected Controller

    C->>A: POST /auth/login
    A->>J: generateAccessToken + generateRefreshToken
    A->>SS: createSession (hashed refresh)
    A-->>C: token + refreshToken

    C->>F: GET /api/... Authorization Bearer access
    F->>J: validate + isAccessToken
    F->>API: SecurityContext set
    API-->>C: 200 Response

    C->>A: POST /auth/refresh { refreshToken }
    A->>SS: validate + rotate session
    A-->>C: new token + refreshToken
```

## Token Structure

| Claim | Purpose |
|-------|---------|
| `sub` | User email |
| `userId` | Database ID |
| `role` | USER/ADMIN/VIEWER |
| `type` | `access` or `refresh` |

## Expiration (Development)

| Type | ms | Duration |
|------|-----|----------|
| access | 86400000 | 24h |
| refresh | 604800000 | 7d |

## Configuration

```properties
jwt.secret=flowiq-dev-secret-key-change-in-production-min-256-bits-long!!
jwt.access-token-expiration=86400000
jwt.refresh-token-expiration=604800000
```

**Production:** Rotate secret via secrets manager; shorten access token TTL.

## Related

- [Authentication Flow](../architecture/flows/authentication-flow.md)
- [Authentication](../security/authentication.md)
- [ADR-006](../architecture/adr/006-jwt-authentication-strategy.md)
