# User Personas

Personas inferred from implemented features, copy, and domain logic.

## 1. Olena — FOP Group 2 IT Freelancer

**Profile:** Software developer, ₴4–5M annual income, no employees, main KVED 62.01.

**Goals:**
- Track income against Group 2 limit (₴5,328,000)
- Know quarterly unified tax deadlines
- Understand when to move to Group 3

**Uses in FlowIQ:**
- Dashboard + `TaxProfileCard` → Business Guide
- Forecast Center → FOP limit forecast
- Tasks → auto tax deadline tasks
- Notifications → 70%/85%/95% limit warnings
- Business Guide → “FOP Group 2”, “unified tax” articles
- FOP Eligibility Checker (client-side)

**Pain points:** Tax rules change; afraid of exceeding limit mid-year.

---

## 2. Andriy — Growing FOP (Group 3)

**Profile:** Agency owner, VAT payer, hires staff, B2B clients.

**Goals:**
- Manage VAT + unified tax 5%
- Track ЄСВ for self and employees
- Export P&L for accountant

**Uses in FlowIQ:**
- Transactions + categories
- Reports (PDF/Excel)
- Analytics → FOP insights
- Tasks → reporting deadlines
- Knowledge → “FOP Group 3 taxes”, VAT articles

---

## 3. Maria — Accountant / Financial Consultant

**Profile:** Serves 10–30 FOP clients (future multi-tenant; today single-user demo).

**Goals:**
- Quick client financial snapshot
- Exportable reports
- Compliance checklist

**Uses in FlowIQ:**
- Reports module
- Analytics overview
- Business Guide as reference for client questions
- Dashboard health score

**Gap:** No multi-client workspace yet; `Role.VIEWER` exists but unused in UI.

---

## 4. Demo User — Evaluator

**Profile:** Prospective customer trying the product.

**Credentials:** `demo@flowiq.ai` / `demo123` (seeded by `DemoUserSeedService`).

**Experience:** Pre-seeded transactions, insights, forecasts from 6-month history (`TransactionSeedService`).

---

## 5. System Administrator (Future)

**Profile:** Internal ops / support.

**Goals:** Monitor health, manage users, review failed imports/reports.

**Uses today:**
- `GET /api/health`
- Swagger UI at `/swagger-ui.html`
- Application logs

**Gap:** No admin UI or user management API beyond self-registration.

## Related Documents

- [Business Requirements](business-requirements.md)
- [Critical User Flows](../qa/critical-user-flows.md)
