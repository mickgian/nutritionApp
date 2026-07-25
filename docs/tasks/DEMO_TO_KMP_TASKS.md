# Meridia — From HTML Demo to Real KMP App

**Status:** Planning · **Owner:** @ottavio (coordination), @egidio (architecture)
**Reference:** the interactive HTML demo — `docs/demo/nutriodemo.html`
**Target:** a production Kotlin Multiplatform client (Android/iOS/Web/Desktop) backed
by the FastAPI service in `backend/`.

This plan turns every flow in the demo into real, tested, end-to-end features. It is
organized into **epics**; each epic is a set of `DEV-XXX` tasks. Tasks follow the
standard template (Problem / Solution / Agents / Change class / Files / Edge cases /
Tests / Acceptance). Work top-to-bottom: an epic's backend + client land together in
one branch/PR, are graded by **@collaudatore**, then merged.

**Conventions** (see `CLAUDE.md`): TDD, ≥80% coverage on new code, Italian UI,
per-user data isolation, money as integer cents, timezone-aware UTC, MVVM with
loading/empty/error/content states.

**Demo entities → domain model**

| Demo concept | Domain entity | Notes |
|--------------|---------------|-------|
| Cliente (persona new/plan/regular) | `User(role=cliente)` | persona = derived state, not stored |
| Dott.ssa Serra | `Professional` / `User(role=admin)` | single studio |
| Prima visita €90 / Controllo €50 | `Appointment` (`VisitType`) | prices in cents |
| Disponibilità (admin) | `AvailabilitySlot` | studio-managed |
| Piano "Dimagrimento 1400 kcal" | `NutritionPlan` | assigned by studio |
| Pasto (kcal + P/C/F) | `Meal` | belongs to a plan/week |
| Box settimanale (14 pasti) | `MealBox` + `BoxItem` | weekly menu |
| Box singolo €89 / abbonamento €79 | `BoxOrder` (`plan=single|subscription`) | pickup slot |
| Ritiro lun mattina/pomeriggio | `Pickup` | on the order |
| Notifiche push profilate | `Notification` | behavior-profiled |
| Credito da cancellazione | `Credit` | valid 6 months |
| Recensioni | `Review` | on the professional |

---

## Epic 0 — Foundations & monorepo

### DEV-001: CI pipeline (GitHub Actions) — ✅ Done
**Problem:** No automated checks; regressions can merge silently.
**Solution:** Two workflows — backend (uv + ruff + pytest + coverage) and KMP
(`./gradlew :shared:allTests` + `:androidApp:assembleDebug`), triggered on PRs.
**Agents:** @silvano (primary), @egidio (review). **Change:** ADDITIVE.
**Files:** `.github/workflows/backend.yml`, `.github/workflows/kmp.yml`.
**Acceptance:** both workflows green on a no-op PR; coverage reported; PR blocked on failure.

### DEV-002: Design system from demo tokens — ✅ Done
**Problem:** The demo's visual language (colors, radii, fonts) must live in Compose.
**Solution:** Encode the demo palette (`--verde #17402F`, `--lime #C9E265`,
`--panna #F7F8F3`, `--arancio #D96C3B`, etc.), radii, and typography into a Compose
`MeridiaTheme` (replace/extend `TravelAppTheme.kt`). Provide reusable components:
`MeridiaButton` (primary/ghost), `Chip`, `Card`, `EyebrowLabel`, `MacroBadge`.
**Agents:** @livia (primary), @gioia (review). **Change:** MODIFYING.
**Files:** `shared/src/commonMain/kotlin/com/meridia/shared/theme/`, reusable components.
**Acceptance:** theme applied app-wide; components render on all four targets; matches demo.

### DEV-003: Package & app identity rename (`com.base` → `com.meridia`) — ✅ Done
**Problem:** The scaffold uses the template package/app name ("Base", `com.base`).
**Solution:** Rename package to `com.meridia`, `rootProject.name` to `Meridia`, app
labels/bundle ids, and the resource package. Do it as one mechanical RESTRUCTURING pass.
**Agents:** @livia (primary), @egidio (review), @silvano (build verify). **Change:** RESTRUCTURING.
**Risks:** broad import churn — verify every target still builds. Do before feature work.
**Acceptance:** all targets compile; no `com.base` references remain; app shows "Meridia".

### DEV-004: Domain schema design (data model ADR) — ✅ Done
**Problem:** Features need a coherent schema before implementation.
**Solution:** @primo + @egidio design the full ERD (entities table above), keys,
indexes, ownership columns, and money-as-cents; record it as an ADR.
**Agents:** @primo (primary), @egidio (approve). **Change:** ADDITIVE (docs).
**Files:** `docs/architecture/decisions/ADR-001-domain-model.md`.
**Acceptance:** ADR approved; each later epic references it; per-user isolation columns defined.

---

## Epic 1 — Authentication & profile

### DEV-010: Auth backend (register / login / me) — ✅ Done
**Problem:** The client has login/registration screens but no real backend.
**Solution:** `POST /api/v1/auth/register`, `POST /api/v1/auth/login` (returns JWT),
`GET /api/v1/auth/me`. Passwords hashed (bcrypt); tokens via `create_access_token`.
**Agents:** @ezio (primary), @primo (User table already scaffolded), @severino, @clelia.
**Change:** ADDITIVE. **Files:** `backend/app/api/v1/auth.py`, `schemas/auth.py`,
`services/auth_service.py`, `repositories/user_repository.py`, tests.
**Error handling:** duplicate email → 409 "Email già registrata"; bad credentials →
401 "Credenziali non valide". **Edge:** empty/invalid email (422), weak password (422).
**Tests:** register happy, duplicate 409, login ok, bad password 401, `me` requires token.
**Acceptance:** ≥80% coverage; Italian errors; JWT round-trips; no plaintext passwords.

### DEV-011: Wire KMP auth to the backend — ✅ Done
**Problem:** `LoginViewModel`/`RegistrationViewModel` target a template API.
**Solution:** Point the Ktor `AuthRepository` at the FastAPI endpoints; store the JWT
in `TokenStorage`; attach it to authorized requests; handle 401 → back to login.
**Agents:** @livia (primary), @clelia. **Change:** MODIFYING.
**Files:** `network/auth/AuthRepository.kt`, `auth/AuthManager.kt`, `viewModels/*`.
**Edge:** offline/network error → Italian error state; expired token → re-login.
**Acceptance:** login/register/logout work against local backend; ViewModel tests for success+error.

---

## Epic 2 — Consultations & booking

### DEV-020: Availability model & studio management (admin) — ✅ Done
**Problem:** Booking needs studio-defined open slots (demo admin toggles times).
**Solution:** `AvailabilitySlot` (date, time, is_open). Admin endpoints
`GET/PUT /api/v1/admin/availability` (require `admin`). @primo migration.
**Agents:** @primo (schema), @ezio (endpoints), @severino (admin role), @clelia.
**Change:** ADDITIVE. **Edge:** past dates rejected; double-booking prevented (409).
**Acceptance:** admin can open/close slots; non-admin gets 403; migration reversible.

### DEV-021: Appointment booking backend — ✅ Done
**Problem:** Clients must book a prima visita / controllo on an open slot.
**Solution:** `GET /api/v1/availability?from=&to=` (open slots), `POST /api/v1/appointments`
(creates a `pending_payment` appointment on an available slot), `GET /api/v1/appointments`
(the caller's own). Price by visit type in cents. Slot becomes unavailable on confirm.
**Agents:** @ezio (primary), @primo, @clelia, @severino. **Change:** ADDITIVE.
**Files:** `api/v1/appointments.py`, `services/appointment_service.py`, `models/appointment.py` (extend), schemas, tests.
**Error handling:** taken slot → 409 "Orario non più disponibile"; unknown slot → 404.
**Edge:** booking another user's appointment (isolation), invalid visit type (422),
concurrent booking of the same slot (409). **Tests:** happy, 409 taken, 404, isolation, edge.
**Acceptance:** ≥80% coverage; ownership enforced; slot lifecycle correct.

### DEV-022: Booking wizard (KMP)
**Problem:** The demo's 3-step wizard (data → riepilogo → pagamento) must be real.
**Solution:** `BookingViewModel` with a stepper `UiState`; a calendar/slot picker fed
by `/availability`; a summary step; a payment step (see Epic 7). Confirmation on success.
**Agents:** @livia (primary), @gioia (UX), @clelia. **Change:** ADDITIVE.
**Files:** `screens/booking/*`, `viewModels/BookingViewModel.kt`, `network/AppointmentRepository.kt`.
**UX states:** loading slots, empty (no availability), error, per-step validation
(continue disabled until valid). All copy Italian. **Cancellation terms** shown (48h rule).
**Acceptance:** end-to-end booking against backend; all states rendered; NAV entry from Consulenza tab.

### DEV-023: Professional profile & reviews
**Problem:** The Consulenza screen shows the nutritionist card + reviews.
**Solution:** `GET /api/v1/professional` (public-ish: bio, credentials, rating,
reviews). Seed Dott.ssa Serra + demo reviews. **Agents:** @ezio, @livia, @primo.
**Change:** ADDITIVE. **Acceptance:** card + reviews render from backend; graceful empty reviews.

---

## Epic 3 — Nutrition plans & meal box

### DEV-030: Nutrition plan assignment (admin) + client view
**Problem:** The box is unlocked only after the studio assigns a plan (persona `new`
sees a locked state).
**Solution:** `NutritionPlan` (name, kcal, weeks). Admin: `POST /api/v1/admin/clients/{id}/plan`.
Client: `GET /api/v1/me/plan`. Persona is derived: no plan → locked box.
**Agents:** @primo, @ezio, @severino (admin + isolation), @clelia. **Change:** ADDITIVE.
**Edge:** client with no plan → 404/empty (client shows locked state, not an error).
**Acceptance:** admin assigns; client sees plan or locked state; isolation enforced.

### DEV-031: Meals & weekly box menu
**Problem:** The box shows 14 meals/week with kcal + macros, per weekday.
**Solution:** `Meal` (title, emoji/asset, kcal, protein_g, carb_g, fat_g, slot=lunch/dinner)
and `MealBox`/`BoxItem` linking meals to a plan + week + day. `GET /api/v1/me/box?week=`.
**Agents:** @primo (schema), @ezio (endpoints), @clelia. **Change:** ADDITIVE.
**Acceptance:** weekday strip + meals render from backend; macros correct; seed one 4-week cycle.

### DEV-032: Meal detail (KMP)
**Problem:** Tapping a meal opens a detail sheet (nutrition, storage, reheating).
**Solution:** `GET /api/v1/meals/{id}`; detail screen with valori nutrizionali,
conservazione, riscaldamento. **Agents:** @livia, @ezio. **Change:** ADDITIVE.
**Acceptance:** detail renders; reachable from the box; Italian copy; loading/error states.

### DEV-033: Box screen with locked/ordered/orderable states (KMP)
**Problem:** The box screen has three states by persona (locked / orderable / ordered).
**Solution:** `BoxViewModel` composing plan + order status; renders locked (no plan),
orderable (deadline banner + "Ordina"), or ordered (in-preparation card).
**Agents:** @livia (primary), @gioia. **Change:** ADDITIVE.
**Acceptance:** all three states correct per backend data; deadline copy; NAV from Box tab.

---

## Epic 4 — Meal-box orders & checkout

### DEV-040: Box order backend
**Problem:** Clients order a single box (€89) or a subscription (€79/box) with a pickup slot.
**Solution:** `POST /api/v1/orders` (plan=single|subscription, pickup=mattina|pomeriggio),
`GET /api/v1/orders` (own), status lifecycle `pending_payment → paid → …`. Prices in cents.
**Agents:** @primo, @ezio, @severino, @clelia. **Change:** ADDITIVE.
**Error handling:** ordering without a plan → 403 "Serve prima un piano"; missing pickup → 422.
**Edge:** duplicate active subscription, isolation, invalid plan/pickup. **Tests:** happy,
403 no-plan, 422, isolation, edge. **Acceptance:** ≥80% coverage; ownership; correct pricing.

### DEV-041: Box checkout (KMP)
**Problem:** The demo's checkout sheet (formula + pickup + pay) must be real.
**Solution:** `BoxCheckoutViewModel`; formula selector, pickup selector, total, payment
(Epic 7); success → box marked ordered. **Agents:** @livia, @gioia, @clelia. **Change:** ADDITIVE.
**Acceptance:** end-to-end order; states rendered; savings/subscription copy correct.

---

## Epic 5 — Profile, cancellation & credits

### DEV-050: Appointment reschedule / cancel + credit
**Problem:** >48h before, a client can move or cancel free and receive a 6-month credit.
**Solution:** `PATCH /api/v1/appointments/{id}` (reschedule), `DELETE /api/v1/appointments/{id}`
(cancel → issue `Credit` if >48h). `GET /api/v1/me/credits`. Enforce the 48h rule server-side.
**Agents:** @ezio (primary), @primo (Credit), @severino, @clelia. **Change:** ADDITIVE.
**Error handling:** <48h cancel → allowed but no refund (documented); reschedule to taken
slot → 409. **Edge:** cancel someone else's appt (403/404), credit expiry. **Tests:** cancel
>48h issues credit, <48h no credit, isolation, reschedule conflict.
**Acceptance:** credit issued correctly; 48h rule enforced server-side (not just UI).

### DEV-051: Profile screen (KMP)
**Problem:** The profilo tab shows appointment, credit, plan, box count, studio info.
**Solution:** `ProfileViewModel` aggregating `/me/*`; manage/cancel actions with the
48h messaging. **Agents:** @livia, @gioia. **Change:** ADDITIVE.
**Acceptance:** renders real data; cancel flow works; empty states; Italian copy.

---

## Epic 6 — Notifications

### DEV-060: Notifications backend + profiling rules
**Problem:** The demo lists behavior-profiled notifications (reminders, order deadline,
box ready, post-consult proposal, win-back, 8-box check-up, birthday, promos).
**Solution:** `Notification` model + `GET /api/v1/me/notifications`. Generation rules
(appointment reminder 24h, order deadline Thursday, box ready Monday, etc.) as a
service; delivery via push is a later integration (out of scope here — model + list first).
**Agents:** @primo, @ezio, @clelia, @egidio (rules review). **Change:** ADDITIVE.
**Acceptance:** notifications listed per user; rules unit-tested; isolation enforced.

### DEV-061: Notifications screen (KMP)
**Solution:** `NotificationsViewModel` + list screen; unread dot on the tab. Italian copy.
**Agents:** @livia, @gioia. **Change:** ADDITIVE.
**Acceptance:** list renders; empty/error states; NAV dot behavior.

---

## Epic 7 — Payments

### DEV-070: Payment abstraction (backend)
**Problem:** Appointments and boxes are paid (demo: Apple/Google Pay + card).
**Solution:** A `PaymentProvider` abstraction that records a `Payment` (amount cents,
method, provider token, status) and confirms the related appointment/order. **Never**
store raw card data — only a provider token. Start with a mock provider behind the
interface; wire a real PSP later. **Agents:** @egidio (design + ADR), @ezio, @severino,
@primo, @clelia. **Change:** ADDITIVE.
**Security:** @severino signs off — no PAN stored, idempotent confirmation, amounts server-authoritative.
**Acceptance:** payment confirms the entity; amounts computed server-side; ADR recorded.

### DEV-071: Payment UI (KMP)
**Solution:** Reusable payment step (Apple/Google Pay + card buttons) used by booking
and box checkout; success/failure states; receipt-by-email copy. **Agents:** @livia, @gioia.
**Change:** ADDITIVE. **Acceptance:** both flows use it; success/failure rendered; Italian.

---

## Epic 8 — Studio (admin) panel

### DEV-080: Admin panel (KMP) + endpoints
**Problem:** The demo's studio panel manages availability, assigns plans, views box
orders, and sends promotions.
**Solution:** Admin-only screens backed by `admin/*` endpoints (availability from
DEV-020, plan assignment from DEV-030, `GET /api/v1/admin/orders`, `POST /api/v1/admin/promotions`).
Gate the whole area behind the `admin` role in the client and enforce on the server.
**Agents:** @livia, @ezio, @severino (role enforcement), @gioia, @clelia. **Change:** ADDITIVE.
**Acceptance:** admin-only access (client + server); each panel action works; isolation/role tested.

---

## Epic 9 — Cross-cutting hardening

### DEV-090: Localization pass
Ensure every user-facing string is Italian (client + API `detail`). @gioia audits, @livia fixes.

### DEV-091: Empty / loading / error states audit
@gioia walks every flow; @collaudatore's `UX_STATES` criterion enforced across screens.

### DEV-092: Accessibility pass
`contentDescription` on meaningful icons/images, touch targets, focus/keyboard (Desktop/Web). @gioia + @livia.

### DEV-093: Security & GDPR review
@severino: health-data minimization, consent, data export & deletion endpoints, JWT
hardening, dependency scan (`pip-audit`, Gradle). ADR for data retention.

### DEV-094: Performance pass
@valerio: API p95 targets, query EXPLAIN/N+1, Compose recomposition, Ktor client reuse.

---

## Suggested sequencing

1. Epic 0 (foundations) → 2. Epic 1 (auth) → 3. Epic 2 (booking) → 4. Epic 3 (plan/box)
→ 5. Epic 4 (orders) → 6. Epic 7 (payments; can interleave with 2 & 4) → 7. Epic 5
(profile/credits) → 8. Epic 6 (notifications) → 9. Epic 8 (admin) → 10. Epic 9 (hardening).

Each `DEV-XXX` is one feature branch, graded by **@collaudatore** against
`.claude/rubrics/feature-implementation.yaml` before merge.
