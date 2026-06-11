# JWT Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant A as AuthController
    participant J as JwtService
    participant F as JwtAuthenticationFilter
    participant API as Protected Controller

    C->>A: POST /auth/login
    A->>J: generateAccessToken(user)
    A->>J: generateRefreshToken(user)
    A-->>C: token + refreshToken

    C->>F: GET /api/... Authorization Bearer
    F->>J: extractUsername + isTokenValid
    F->>F: isAccessToken (reject refresh)
    F->>API: SecurityContext set
    API-->>C: 200 Response
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

## Refresh Gap

Refresh tokens issued but **no `/auth/refresh` endpoint**. Frontend `refreshToken()` throws not implemented.

## Related

- [Authentication](authentication.md)
