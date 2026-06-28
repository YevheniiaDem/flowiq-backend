# FlowIQ Production Security Audit

| Field | Value |
|-------|-------|
| **Audit date** | 2026-06-28 |
| **Scope** | `flowiq-backend`, `flowiq-frontend`, `flowiq-automation`, CI/CD, Docker |
| **Method** | Static code review, configuration analysis, architecture inspection |
| **Classification** | Critical · High · Medium · Low |

---

## Executive Summary

FlowIQ implements a **solid baseline** for a JWT-based SPA + API stack: BCrypt password hashing, refresh token rotation with server-side session hashes, JPA parameterized queries, global exception sanitization, audit logging with metadata redaction, and non-root Docker runtime.

**Primary production risks** are operational rather than implementation bugs:

1. **Committed default secrets** (JWT, DB) must never reach production unchanged.
2. **Demo account auto-seeding** with a known password when enabled.
3. **No RBAC enforcement** despite roles in the data model.
4. **No rate limiting** on authentication endpoints.
5. **JWT stored in `localStorage`** (XSS exfiltration surface).

### Remediation applied in this audit

| Fix | Severity addressed |
|-----|-------------------|
| Removed demo password from application logs | High |
| Added `application-prod.properties` with hardened defaults + fail-fast validator | Critical |
| Hardened `application-docker.properties` (demo off, Swagger off, env-based secrets) | High |
| Avatar filename allowlist regex (defense in depth) | Medium |

---

## Findings Summary

| Severity | Count | Fixed in audit |
|----------|-------|----------------|
| **Critical** | 2 | 1 mitigated (prod fail-fast + profile) |
| **High** | 9 | 3 mitigated |
| **Medium** | 14 | 1 mitigated |
| **Low** | 11 | 0 |

---

## 1. JWT (JSON Web Tokens)

### Implemented (strengths)

| Control | Status | Evidence |
|---------|--------|----------|
| HS256 signed tokens | ✅ | `JwtService`, jjwt 0.12.6 |
| Access vs refresh separation (`type` claim) | ✅ | `isAccessToken()`, `isRefreshToken()` |
| Filter rejects refresh tokens on API calls | ✅ | `JwtAuthenticationFilter` |
| Claims: `sub`, `userId`, `role`, `sessionId` | ✅ | `JwtService.generateToken()` |
| Invalid token clears security context | ✅ | Filter catch block |

### Findings

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| JWT-01 | **Critical** | Default `jwt.secret` committed in `application.properties`. Anyone with repo access can forge tokens if reused in production. | Set `JWT_SECRET` via secrets manager. Use `SPRING_PROFILES_ACTIVE=prod` — `ProductionSecretsValidator` now **fail-fast** on weak secrets. |
| JWT-02 | **High** | Access token lifetime **24 hours** in default config. Stolen access token valid for long window. | Prod profile defaults to **15 minutes** (`application-prod.properties`). Override via `JWT_ACCESS_TOKEN_EXPIRATION`. |
| JWT-03 | **Medium** | Signing key derived via Base64-encoding raw string bytes — non-standard but functional if secret is long enough. | Prefer cryptographically random 256+ bit secret from env; consider `Keys.secretKeyFor(SignatureAlgorithm.HS256)`. |
| JWT-04 | **Low** | No `aud` / `iss` claims for multi-service validation. | Add when multiple services consume tokens. |

---

## 2. Refresh Tokens

### Implemented (strengths)

| Control | Status | Evidence |
|---------|--------|----------|
| Refresh endpoint | ✅ | `POST /api/auth/refresh` |
| Rotation on each refresh | ✅ | `SessionService.rotateRefreshToken()` |
| Server-side hash storage (SHA-256) | ✅ | `user_sessions.refresh_token_hash` |
| Session binding via `sessionId` claim | ✅ | Login + refresh flow |
| Disabled user rejected | ✅ | `AuthService.refresh()` |
| Audit on refresh | ✅ | `AUTH_REFRESH` event |

### Findings

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| RT-01 | **High** | No **refresh token reuse detection** — if a revoked/old refresh is presented, session is not automatically revoked cluster-wide. | On hash mismatch with valid JWT structure, revoke session and log `AUTH_REFRESH_REUSE`. |
| RT-02 | **Medium** | Refresh lifetime **7 days** — long window for stolen refresh token. | Consider 24–72h for production; require re-login for sensitive actions. |
| RT-03 | **Low** | SHA-256 for token hash (not HMAC with pepper). Acceptable for high-entropy tokens. | Optional: add server-side pepper in hash. |

---

## 3. Sessions

### Implemented (strengths)

| Control | Status | Evidence |
|---------|--------|----------|
| Session CRUD | ✅ | `SessionService`, `user_sessions` table |
| Logout revokes current session | ✅ | `AuthService.logout()` |
| Logout all / others | ✅ | Profile session endpoints |
| User-agent + IP stored | ✅ | `createSession()` |
| Revoked sessions excluded | ✅ | `revoked_at IS NULL` queries |

### Findings

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| SES-01 | **Medium** | Client IP from `X-Forwarded-For` without trusted proxy validation — spoofable if backend exposed directly. | Configure `ForwardedHeaderFilter` with trusted proxy CIDRs only. |
| SES-02 | **Low** | Access token use does not update `last_activity_at` on every request. | Optional: touch session in filter for session management UI accuracy. |

---

## 4. Password Storage & BCrypt

### Implemented (strengths)

| Control | Status | Evidence |
|---------|--------|----------|
| BCrypt via Spring Security | ✅ | `BCryptPasswordEncoder` in `SecurityConfig` |
| Passwords never returned in API | ✅ | `UserResponse` excludes password |
| Login uses `AuthenticationManager` | ✅ | Constant-time compare via BCrypt |
| Change password complexity rules | ✅ | `ChangePasswordRequest` — min 10, upper/lower/digit/special |

### Findings

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| PWD-01 | **Medium** | Registration allows **min 6 characters**; change-password requires **min 10 + complexity**. Inconsistent policy. | Align register to min 10 + same `@Pattern` as change-password. |
| PWD-02 | **Low** | BCrypt default strength **10** (2^10 rounds). | Increase to 12 for production if latency acceptable. |
| PWD-03 | **High** | No account lockout after failed login attempts. | Add rate limiting + temporary lockout (see RL-01). |

---

## 5. Authorization & RBAC

### Implemented (strengths)

| Control | Status | Evidence |
|---------|--------|----------|
| Authentication required for `/api/**` (except allowlist) | ✅ | `SecurityConfig` |
| User-scoped data in services | ✅ | JWT → email → user ID |
| Roles stored: `USER`, `ADMIN`, `VIEWER` | ✅ | `User.Role` enum |
| Authorities in JWT / `UserPrincipal` | ✅ | `ROLE_*` granted |

### Findings

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| RBAC-01 | **High** | **No `@PreAuthorize` / `@Secured`** — all authenticated users access all endpoints. ADMIN/VIEWER roles are **inert**. | Add method-level security for admin/future multi-tenant features. |
| RBAC-02 | **High** | IDOR relies entirely on service-layer user scoping — no centralized enforcement. | Add integration tests per resource; consider `@PreAuthorize("@authz.isOwner(#id)")`. |
| RBAC-03 | **Medium** | Role claim in JWT not revalidated against DB on each request — role change takes effect only after token expiry. | Reload role from DB in filter or shorten access token TTL (prod profile helps). |

---

## 6. CORS

### Implemented (strengths)

| Control | Status | Evidence |
|---------|--------|----------|
| Explicit origin allowlist (no `*`) | ✅ | `CorsConfig` |
| Credentials allowed with named origins | ✅ | Correct pattern |
| Scoped to `/api/**` | ✅ | |
| Custom headers allowed | ✅ | `X-App-Language`, `X-App-Currency` |

### Findings

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| CORS-01 | **Medium** | Origins **hardcoded** — new production domains require code change + redeploy. | Externalize via `FLOWIQ_CORS_ORIGINS` env var. |
| CORS-02 | **Low** | `PATCH` allowed but rarely used. | Remove unused methods if desired. |

---

## 7. CSRF

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| CSRF-01 | **Low** | CSRF **disabled** — `AbstractHttpConfigurer::disable`. | **Acceptable** for stateless JWT Bearer API. Document that cookies are not used for auth. Re-enable only if cookie-based auth is added. |

---

## 8. Rate Limiting

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| RL-01 | **High** | **No rate limiting** on `/api/auth/login`, `/register`, `/refresh`. Vulnerable to credential stuffing and registration spam. | Add Bucket4j, Spring Cloud Gateway, or reverse-proxy rate limits (e.g. 5 login/min/IP). |
| RL-02 | **Medium** | No rate limiting on file upload or report generation — DoS via resource exhaustion. | Limit uploads/reports per user per hour. |

---

## 9. Input Validation

### Implemented (strengths)

| Control | Status | Evidence |
|---------|--------|----------|
| Jakarta Validation on DTOs | ✅ | `@Valid` on controllers |
| Centralized validation errors | ✅ | `GlobalExceptionHandler` |
| Transaction date validation | ✅ | `TransactionDateValidator` |
| CSV size limit 10 MB | ✅ | `ImportService`, multipart config |
| Avatar size limit 5 MB | ✅ | `AvatarStorageService` |

### Findings

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| VAL-01 | **Medium** | CSV import validates extension only — content-type spoofing possible. | Validate parse result; reject binary content. |
| VAL-02 | **Medium** | Search/query params may lack `@Size` limits on some endpoints. | Audit all `@RequestParam String` for max length. |

---

## 10. SQL Injection Protection

### Implemented (strengths)

| Control | Status | Evidence |
|---------|--------|----------|
| Spring Data JPA / JPQL only | ✅ | All `@Query` use named parameters |
| No native SQL queries found | ✅ | Grep audit |
| Hibernate `ddl-auto=validate` | ✅ | No dynamic DDL |

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| SQL-01 | **Low** | Risk low with current codebase. | Continue banning native queries without review. |

---

## 11. XSS Protection

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| XSS-01 | **High** | JWT access + refresh tokens in **`localStorage`** — any XSS exfiltrates session. | Migrate to **HttpOnly Secure SameSite cookies** or use short-lived tokens + strict CSP. |
| XSS-02 | **Medium** | No **Content-Security-Policy** header on frontend or API responses. | Add CSP via Next.js headers / reverse proxy. |
| XSS-03 | **Low** | React default escaping mitigates stored XSS in UI. | Avoid `dangerouslySetInnerHTML`; audit user-generated content fields. |

---

## 12. File Upload Security

### Implemented (strengths)

| Control | Status | Evidence |
|---------|--------|----------|
| Max upload 10 MB (global) | ✅ | `application.properties` |
| Avatar content-type allowlist | ✅ | JPEG, PNG, WebP, GIF |
| Path traversal prevention | ✅ | `normalize()` + `startsWith()` check |
| Avatar filename allowlist | ✅ **Fixed** | Regex `^[0-9]+_[uuid].(ext)$` |
| Server-generated avatar filenames | ✅ | UUID in name |

### Findings

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| UPL-01 | **Medium** | Avatar validation trusts client `Content-Type` — no magic-byte verification. | Validate file signatures (e.g. Apache Tika or first bytes check). |
| UPL-02 | **Medium** | Avatar endpoint **public** (`/api/profile/avatars/**`) — UUID filenames reduce guessability but URLs may leak via referrer/logs. | Consider authenticated avatar delivery or signed URLs. |
| UPL-03 | **Low** | CSV stored in memory only (not persisted) — good. | Monitor memory for large uploads under concurrency. |

---

## 13. Logging & Sensitive Information Exposure

### Implemented (strengths)

| Control | Status | Evidence |
|---------|--------|----------|
| Generic 500 message | ✅ | `"An unexpected error occurred"` |
| Login failure message generic | ✅ | `"Invalid email or password"` |
| Audit metadata sanitizer | ✅ | Strips password/token/file content keys |
| Password excluded from API responses | ✅ | |

### Findings

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| LOG-01 | **High** | Demo password logged in plaintext — **Fixed** (`DemoUserSeedService`). | Scan logs for historical leakage. |
| LOG-02 | **Medium** | `spring.jpa.show-sql=true` in default `application.properties` — SQL logged in dev. | Disabled in `docker` and `prod` profiles. |
| LOG-03 | **Medium** | Swagger UI **publicly accessible** in default profile. | Disabled in `docker` and `prod` profiles. |
| LOG-04 | **Low** | Correlation IDs present — good for forensics. | Ensure log aggregation redacts PII. |

---

## 14. Secrets Management

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| SEC-01 | **Critical** | JWT + DB credentials in committed properties. | **Never deploy default profile alone.** Use `prod` profile + env vars. See [SECRETS_AUDIT.md](SECRETS_AUDIT.md). |
| SEC-02 | **High** | Demo user `demo@flowiq.ai` / `demo123` seeded when `flowiq.demo-seed.enabled=true` (**default**). | Disable in production (`application-prod.properties`, `application-docker.properties` now default `false`). |
| SEC-03 | **Medium** | No `.env` gitignore issue found — good. | Add secret scanning in CI (Gitleaks, GitHub secret scanning). |

### Production secret checklist

```bash
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET="<256-bit-random>"
export SPRING_DATASOURCE_URL="jdbc:postgresql://..."
export SPRING_DATASOURCE_USERNAME="..."
export SPRING_DATASOURCE_PASSWORD="..."
export SPRING_DATASOURCE_PASSWORD="..."  # strong, unique
export FLOWIQ_DEMO_SEED_ENABLED=false      # via flowiq.demo-seed.enabled
```

---

## 15. Docker Security

### Implemented (strengths)

| Control | Status | Evidence |
|---------|--------|----------|
| Multi-stage build | ✅ | `Dockerfile` |
| Non-root user `flowiq` | ✅ | `USER flowiq` |
| Healthcheck | ✅ | `/api/health` |
| Tests skipped in image build | ✅ | `-DskipTests` (CI runs verify separately) |

### Findings

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| DOC-01 | **Medium** | `compose.yaml` uses weak password `flowiq123` — acceptable for **local dev only**. | Never reuse in production PostgreSQL. |
| DOC-02 | **Medium** | No read-only root filesystem or dropped capabilities in Dockerfile. | Add `readOnlyRootFilesystem`, `no-new-privileges` in orchestrator. |
| DOC-03 | **Low** | JRE Alpine base — keep updated for CVE patches. | Pin digest; scan images in CI. |

---

## 16. GitHub Actions Security

### Implemented (strengths)

| Control | Status | Evidence |
|---------|--------|----------|
| Minimal permissions (`contents: read`) | ✅ | `backend-ci.yml`, `frontend-ci.yml`, `pr-validation.yml` |
| Pinned action major versions (`@v4`) | ✅ | checkout, setup-java, setup-node |
| No secrets in workflow logs (reviewed) | ✅ | |
| Test credentials via GitHub Secrets | ✅ | automation smoke/nightly |

### Findings

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| GHA-01 | **Medium** | **No dependency vulnerability scanning** in any repo CI. | Add OWASP Dependency-Check, Snyk, or GitHub Dependabot alerts. |
| GHA-02 | **Medium** | Hardcoded `flowiq123` in automation CI Postgres service — isolated ephemeral DB. | Acceptable for CI; document scope. |
| GHA-03 | **Low** | `GH_PAT` optional for cross-repo checkout — scope PAT minimally. | Use fine-grained PAT with read-only on backend repo. |
| GHA-04 | **Low** | No `pull_request` workflow permission hardening (`pull_request_target` not used — good). | Continue avoiding `pull_request_target` with checkout of PR code. |

---

## 17. Dependency Vulnerabilities

| Dependency | Version | Notes |
|------------|---------|-------|
| Spring Boot | 3.5.14 | Current; inherits managed dependency CVE fixes |
| jjwt | 0.12.6 | Recent |
| PostgreSQL driver | Managed by Boot | |
| Apache POI | 5.4.0 | Monitor CVE advisories |
| OpenPDF | 2.0.3 | Monitor advisories |
| springdoc-openapi | 2.8.17 | |

| ID | Severity | Finding | Recommendation |
|----|----------|---------|----------------|
| DEP-01 | **High** | **No automated dependency scanning** in CI/CD pipeline. | Enable Dependabot + weekly `mvn org.owasp:dependency-check-maven:check`. |
| DEP-02 | **Low** | `spring-boot-devtools` present as optional runtime — disabled in packaged prod by Boot. | Verify devtools not in production classpath. |

---

## 18. Security Control Matrix

| Domain | Status | Priority fix |
|--------|--------|--------------|
| JWT signing | ⚠️ Config-dependent | `JWT_SECRET` in prod |
| Refresh rotation | ✅ Good | Reuse detection |
| Sessions | ✅ Good | Trusted proxy config |
| BCrypt | ✅ Good | Align register policy |
| RBAC | ❌ Not enforced | `@PreAuthorize` |
| CORS | ✅ Good | Externalize origins |
| CSRF | ✅ N/A (JWT) | — |
| Rate limiting | ❌ Missing | Auth endpoints |
| Validation | ⚠️ Partial | Register password |
| SQL injection | ✅ Good | — |
| XSS | ⚠️ localStorage | CSP + cookies |
| File upload | ⚠️ Improved | Magic bytes |
| Logging | ✅ Improved | — |
| Secrets | ⚠️ Config-dependent | prod profile |
| Docker | ⚠️ Partial | Prod DB password |
| CI/CD | ⚠️ Partial | Dependency scan |
| Dependencies | ⚠️ Unscanned | OWASP/Dependabot |

---

## 19. Remediation Roadmap

### Immediate (before production launch)

1. Deploy with `SPRING_PROFILES_ACTIVE=prod` and unique `JWT_SECRET` / DB credentials.
2. Confirm `flowiq.demo-seed.enabled=false`.
3. Confirm Swagger/OpenAPI disabled (`springdoc.*.enabled=false`).
4. Enable HTTPS termination (Vercel / reverse proxy).

### Short term (1–2 sprints)

1. Rate limiting on auth endpoints (RL-01).
2. Dependabot + OWASP dependency-check in CI (DEP-01).
3. Externalize CORS origins (CORS-01).
4. Align registration password policy with change-password (PWD-01).
5. Refresh token reuse detection (RT-01).

### Medium term

1. RBAC enforcement for ADMIN/VIEWER (RBAC-01).
2. CSP headers + XSS hardening (XSS-02).
3. Evaluate HttpOnly cookie auth strategy (XSS-01).
4. Avatar magic-byte validation (UPL-01).

---

## 20. Related Documents

| Document | Purpose |
|----------|---------|
| [SECRETS_AUDIT.md](SECRETS_AUDIT.md) | Secrets inventory |
| [authentication.md](authentication.md) | Auth module |
| [authorization.md](authorization.md) | Authorization design |
| [JWT_STORAGE_SECURITY_REVIEW.md](JWT_STORAGE_SECURITY_REVIEW.md) | Frontend token storage |
| [AUDIT_LOG_DESIGN.md](AUDIT_LOG_DESIGN.md) | Audit logging |
| [../architecture/flows/authentication-flow.md](../architecture/flows/authentication-flow.md) | Auth sequence diagrams |
| [../product/SRS.md](../product/SRS.md) §6 | Security requirements |

---

## Appendix: Files Changed in This Audit

| File | Change |
|------|--------|
| `DemoUserSeedService.java` | Removed password from log output |
| `ProductionSecretsValidator.java` | Fail-fast on weak JWT secret in `prod` profile |
| `application-prod.properties` | Hardened production defaults |
| `application-docker.properties` | Demo/Swagger off; env-based secrets |
| `AvatarStorageService.java` | Filename allowlist regex |
| `ProductionSecretsValidatorTest.java` | Unit tests |
| `AvatarStorageServiceTest.java` | Updated for filename pattern |

---

*End of security audit — 2026-06-28.*
