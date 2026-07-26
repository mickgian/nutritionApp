# DEV-093 — Security & GDPR review

Owner: @severino · Scope: Epic 9 hardening. This note records the review and the
decisions behind the code changes. The data-retention rationale (ADR-003) lives
as the module docstring of `backend/app/services/privacy_service.py`.

## 1. GDPR data-subject rights (implemented)

Two self-service endpoints, both acting **only on the caller** (`CurrentUser`,
Rule 4 — a client can never export or erase anyone else's data):

- **`GET /api/v1/me/export`** — right to access/portability (Art. 15/20).
  Returns one JSON document with the caller's account, appointments, orders,
  payments, nutrition plans, meal boxes, credits, notifications and reviews.
  Secrets that are Meridia's, not the user's, are **never** included: password
  hashes and payment `provider_token` references are excluded by construction
  (the export schemas simply don't carry them).
- **`DELETE /api/v1/me`** — right to erasure (Art. 17). Hard-deletes the caller's
  health and behavioural records and **anonymises** the account row (email →
  non-routable tombstone, name cleared, password made unusable, `is_active` →
  False). Deletion runs in foreign-key-safe order and releases any availability
  slot the client's appointments were holding, so the studio calendar frees up.
  A tombstoned account cannot authenticate. See ADR-003 for why anonymise (not
  drop) the row, and why no accounting-retention exception is currently taken.

## 2. Health-data minimisation & consent

- Nutrition plans are health data; they are erased immediately on request and are
  scoped per-client everywhere (Rule 4). The export lets a client see exactly
  what is held.
- Consent today is implicit at registration. **Follow-up (not blocking):** add an
  explicit privacy-policy consent checkbox to the registration flow and record
  the consent timestamp. Tracked for a later task.

## 3. JWT hardening (implemented)

- `decode_token` now requires **both `exp` and `sub`**: a token missing an expiry
  (a never-expiring token) or a subject is rejected instead of trusted. Expired
  tokens were already rejected; this is covered by tests in `tests/test_security.py`.
- **Fail-closed production secret guard** (`app/core/config.py`): the app refuses
  to boot in `production` if `SECRET_KEY` is still the `.env.example` placeholder
  or shorter than 32 chars. Development/test are unaffected.
- Unchanged and confirmed good: HS256 + bcrypt, 60-min access-token expiry,
  per-user isolation via `get_current_user` / `require_admin`.

## 4. Dependency scan

### Backend — `pip-audit`
One finding: **`ecdsa` 0.19.2 — PYSEC-2026-1325** (timing side-channel / Minerva),
pulled in transitively by `python-jose`. **No fix version is available upstream.**

**Assessment: not exploitable in Meridia's configuration.** All JWTs are signed
and verified with **HS256 (HMAC-SHA256)**, which uses no elliptic-curve code —
the `ecdsa` package is never exercised. Accepted with monitoring.

**Follow-up (not blocking):** migrating JWT handling from `python-jose` to `PyJWT`
would drop the `ecdsa` dependency entirely and move onto a more actively
maintained library. This is an architectural change (needs @egidio) and is
tracked separately.

### Frontend — Gradle
Could not run in this environment (Gradle is offline/proxy-blocked here; see the
DEV-001 KMP CI notes). **Recommendation:** enable Dependabot for Gradle and/or add
a dependency-vulnerability check to the `kmp.yml` CI job.

## 5. Client surface

The Profilo tab exposes a "Privacy e dati" card (`ProfilePrivacyCard`) that
consumes both endpoints: "Esporta i miei dati" (export) and "Elimina account"
(erasure, behind an explicit in-card confirmation, followed by logout).

## Test coverage

`tests/test_privacy.py` (8) + `tests/test_security.py` (JWT/config, 7) — 100%
line coverage on the new/changed backend modules; `ProfileViewModelTest` adds the
export/erasure client cases.
