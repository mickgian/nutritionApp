# DEV-094 — Performance pass

Owner: @valerio · Epic 9 hardening. Audit of query efficiency, client reuse, and
recomposition, plus the fixes applied.

## Backend — query efficiency

### N+1 fixed (client hot path)
- **`GET /me/box`** — `BoxService._assemble` fetched one meal per box cell via
  `MealRepository.get_by_id` (up to 14 queries per box read). Now it batch-loads
  every referenced meal in **one** query (`MealRepository.list_by_ids`, using
  `WHERE id IN (...)`). A box read drops from ~1+14 to 2 queries.
- **`MealRepository.count`** loaded every meal row and took `len(...)` just to
  check whether the catalog was seeded. Now issues a real `SELECT COUNT(*)`.
- **`DELETE /me` (erasure)** — box-item deletion issued one query per box. Now a
  single `WHERE box_id IN (...)` query removes all items.

### Indexes reviewed (no change needed)
Every client-owned table indexes `client_id` (appointments, orders, payments,
credits, notifications, nutrition_plans, meal_boxes); `appointments.scheduled_at`
and `availability_slots.starts_at` are indexed; notifications carry a unique
`(client_id, dedupe_key)`. All ownership filters (`WHERE client_id = ?`) hit an
index. No missing index found.

### Not runnable in this sandbox
`EXPLAIN`/`EXPLAIN ANALYZE` needs Postgres, which is unavailable here (tests run
on in-memory SQLite). **Recommendation:** confirm the plans for the box/plan and
availability queries against real Postgres in CI/staging.

### API p95 targets (guidance to verify under load)
- Read endpoints (`GET /me/box`, `/me/plan`, `/me/notifications`, `/me/credits`,
  `/availability`): target **p95 < 150 ms**.
- Writes (`POST /appointments`, `/orders`, `/payments`, `DELETE /me`): target
  **p95 < 300 ms**.
Measure in staging with representative data; the box read was the main risk and
is now bounded.

## Frontend — Ktor client reuse (confirmed good)
A single Ktor `HttpClient` lives in the `HttpClientProvider` object and is the
default constructor arg of **every** repository — no per-call client creation
(grep confirms only one `HttpClient { }` in the module). Timeouts are configured
(connect 15 s, socket/request 60 s). No change needed.

## Frontend — Compose recomposition
Lists (notifications, orders, clients, availability slots, box meals) are small
and rendered eagerly with `Column` + `forEach`; there is no `LazyColumn` in the
app. Screens read state narrowly via a single `collectAsState()` per screen, so
recomposition is scoped to the screen. **Recommendation:** if any list can grow
unbounded (e.g. a full notifications history), migrate it to `LazyColumn` with
stable item keys (`key = { it.id }`) to keep scrolling and recomposition cheap.
