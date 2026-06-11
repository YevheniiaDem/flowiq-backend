# Authentication API

**Controller:** `com.flowiq.controller.AuthController`  
**Base path:** `/api/auth`

## Endpoints

### POST `/api/auth/register`

**Auth:** Public

**Request:**
```json
{
  "email": "user@example.com",
  "password": "securePass123",
  "name": "Olena Kovalenko",
  "company": "My FOP"
}
```

**Response:** `201` — `AuthResponse`
```json
{
  "user": { "id": "2", "email": "user@example.com", "name": "Olena Kovalenko", "role": "user" },
  "token": "<jwt_access>",
  "refreshToken": "<jwt_refresh>"
}
```

---

### POST `/api/auth/login`

**Auth:** Public

**Request:**
```json
{
  "email": "demo@flowiq.ai",
  "password": "demo123"
}
```

**Response:** `200` — `AuthResponse` (same shape as register)

**Errors:** `401` — Invalid email or password

---

### GET `/api/auth/me`

**Auth:** JWT required

**Response:** `200` — `UserResponse`

---

### POST `/api/auth/logout`

**Auth:** JWT required

**Response:** `204` No Content

## JWT Details

See [JWT Flow](../security/jwt-flow.md).

| Token | Expiration (dev) |
|-------|------------------|
| Access | 24 hours |
| Refresh | 7 days |

**Claims:** `sub` (email), `userId`, `role`, `type` (`access` | `refresh`)

## Frontend Integration

`src/services/auth.service.ts` stores `token`, `refreshToken`, `user` in `localStorage`.

## Related

- [Authentication Module](../modules/authentication.md)
- [Security: Authentication](../security/authentication.md)
