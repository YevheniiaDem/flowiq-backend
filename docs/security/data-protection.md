# Data Protection

## Data Classification

| Data | Sensitivity | Storage |
|------|-------------|---------|
| Passwords | High | BCrypt hash in `users.password` |
| JWT secret | Critical | `application.properties` (dev only) |
| Transactions | Business confidential | PostgreSQL per-user |
| Report files | Business confidential | BYTEA in `report_jobs` |
| Knowledge articles | Public regulatory info | PostgreSQL global |

## Transport

HTTPS required in production (terminated at load balancer / Vercel).

## Headers

- `Authorization: Bearer` — not logged by default
- `X-App-Language`, `X-App-Currency` — preferences only

## PII Fields

`users.email`, `users.name`, `users.company`

## Recommendations

- [ ] Encrypt `report_jobs.file_content` at rest
- [ ] Remove demo credentials from production builds
- [ ] GDPR: user data export & deletion endpoints
- [ ] Audit log for admin access
- [ ] Rate limit `/auth/login`

## Ukrainian Compliance Note

Tax and financial data may fall under local accounting confidentiality — document data processing agreement for B2B accountant access.

## Related

- [Privacy Policy Draft](../legal/privacy-policy-draft.md)
