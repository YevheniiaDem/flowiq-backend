# Authentication Module

**Backend:** `AuthController`, `AuthService`, `security.*`  
**Frontend:** `features/auth/`

## Flow

```mermaid
sequenceDiagram
    participant U as User
    participant LF as LoginForm
    participant AS as authService
    participant API as AuthController
    participant JWT as JwtService

    U->>LF: email + password
    LF->>AS: login()
    AS->>API: POST /auth/login
    API->>JWT: generateAccessToken + refresh
    API-->>AS: AuthResponse
    AS->>AS: localStorage token/user
    LF->>U: redirect /
```

## Password Storage

BCrypt via `BCryptPasswordEncoder` in `SecurityConfig`.

## Demo Account

`DemoUserSeedService` creates `demo@flowiq.ai` / `demo123` on startup.

## Protected Routes

All `/api/**` except health and login/register require valid access JWT.

Frontend: `MainLayout` client-side guard.

## Related

- [Authentication API](../api/authentication-api.md)
- [JWT Flow](../security/jwt-flow.md)
