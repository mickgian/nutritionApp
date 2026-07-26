# ADR-002 — Payment abstraction

- **Status:** Accepted
- **Date:** 2026-07-26
- **Deciders:** @egidio (architect, author + approve)
- **Consulted:** @ezio (backend), @severino (security & PCI scope), @primo (schema)
- **Task:** DEV-070 · **Supersedes:** — · **Extends:** ADR-001 (domain model)

> How Meridia takes money for appointments and box orders without ever handling
> raw card data. Extends ADR-001 with the `payments` table.

---

## Context

Appointments (prima €90 / controllo €50) and box orders (singolo €89 /
abbonamento €79) are created in `pending_payment` and must be paid to become
usable. The demo shows Apple Pay / Google Pay / card. We need to (a) take a
payment, (b) confirm the related entity, (c) never touch a card number, and
(d) keep a real PSP swappable in later without reworking callers.

### Forces

- **No PAN, ever (PCI scope).** The card number is tokenized on the device
  (Apple/Google Pay, or a PSP card element). The backend receives only an opaque
  **provider token**, passes it to the provider, and stores only the provider's
  charge **reference** — never a PAN, CVV, or expiry. This keeps the backend out
  of PCI-DSS cardholder-data scope.
- **Amounts server-authoritative (Rule 7).** The charged amount is read from the
  target entity (`appointment.price_cents` / `order.price_cents`), never from the
  request. The pay request carries no amount field at all.
- **Idempotent confirmation.** Paying an already-paid entity must not double
  charge: the service short-circuits on the terminal state and returns the
  existing successful payment.
- **Swappable provider.** A `PaymentProvider` protocol (`charge(token, amount) →
  PaymentResult`) hides the PSP. Ship a `MockPaymentProvider`; wire a real PSP
  behind the same interface later with no change to the service or callers.

## Decision

A `payments` table records every charge attempt:

| Column | Type | Notes |
|--------|------|-------|
| `id` | int PK | surrogate |
| `client_id` | int FK users.id, **indexed** | ownership (Rule 4) |
| `appointment_id` | int? FK appointments.id | exactly one target set |
| `order_id` | int? FK orders.id | exactly one target set |
| `amount_cents` | int | server-authoritative, copied from the target |
| `method` | enum `apple_pay\|google_pay\|card` | how the client paid |
| `provider` | str | e.g. `mock` |
| `provider_token` | str | the PSP charge **reference** — never a PAN |
| `status` | enum `succeeded\|failed` | failures are recorded for audit |
| `created_at` | datetime UTC | |

Flow (`POST /api/v1/payments`, body `{target, target_id, method, token}`):

1. Load the target (appointment or order); 404 unless it belongs to the caller.
2. 409 if the target is cancelled.
3. If the target is already `confirmed`/`paid`, return its existing successful
   payment (idempotent, no re-charge).
4. Charge `provider.charge(token, entity.price_cents)`. Record a `Payment` with
   the outcome. On failure record `status=failed` and raise **402**. On success
   set the target to `confirmed` (appointment) / `paid` (order) and return it.

## Consequences

- The backend never stores cardholder data; only a provider reference. @severino
  signs off on PCI scope.
- Amounts cannot be tampered with from the client (no amount in the request).
- A real PSP is a new `PaymentProvider` implementation + config; callers, schema,
  and the endpoint are unchanged.
- Failed attempts are auditable (`status=failed`) without confirming the entity.
