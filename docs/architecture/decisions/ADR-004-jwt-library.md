# ADR-004 — JWT library: python-jose → PyJWT

- **Status:** Accepted
- **Date:** 2026-07-26
- **Deciders:** @egidio (architect, author + approve)
- **Consulted:** @severino (security & dependency scan), @ezio (backend)
- **Task:** DEV-096 · **Supersedes:** — · **Relates to:** DEV-093 (security review)

> Replace `python-jose` with `PyJWT` for JWT encode/decode so the vulnerable,
> unused transitive `ecdsa` dependency leaves the tree.

---

## Context

The DEV-093 dependency scan (`pip-audit`) flagged **`ecdsa` 0.19.2 —
PYSEC-2026-1325** (Minerva timing side-channel), pulled in transitively by
`python-jose[cryptography]`. **No fixed `ecdsa` release exists upstream.**

The finding was assessed **non-exploitable** in Meridia: every JWT is signed and
verified with **HS256 (HMAC-SHA256)**, which exercises no elliptic-curve code, so
`ecdsa` is never called at runtime. It was accepted with monitoring, and a
library migration was tracked as the durable fix.

### Forces

- **Shrink the attack surface.** A vulnerable package that ships but is never
  called is still a supply-chain and audit liability. Removing it is strictly
  better than annotating it as a false positive forever.
- **Active maintenance.** `PyJWT` is the more actively maintained library and has
  no `ecdsa` dependency; HS256 is handled by the standard library `hmac`.
- **Contained blast radius.** All JWT handling lives behind two functions in
  `app/core/security.py` (`create_access_token`, `decode_token`). No other module
  imports `jose`. The swap is a library change, not a design change: same HS256,
  same claims (`sub`, `role`, `exp`), same "require both `exp` and `sub`" policy,
  same fail-closed behaviour (`decode_token` returns `None` on any invalid token).
- **No token-format change.** HS256 tokens are library-agnostic; tokens minted by
  either library validate under the other. No re-issue, no client change, no
  migration of stored tokens (there are none — tokens are stateless).

## Decision

Replace the dependency and rewrite only the two helper functions:

| Aspect | Before (`python-jose`) | After (`PyJWT`) |
|--------|------------------------|-----------------|
| Import | `from jose import JWTError, jwt` | `import jwt` (`PyJWTError` base) |
| Encode | `jwt.encode(payload, key, algorithm=...)` | identical call shape |
| Decode | `options={"require_exp": True, "require_sub": True}` | `options={"require": ["exp", "sub"]}` |
| Error caught | `JWTError` | `jwt.PyJWTError` (covers expired / missing-claim / decode errors) |

`pyproject.toml`: drop `python-jose[cryptography]`, add `pyjwt>=2.10` (2.10+ also
validates that `sub` is a string — Meridia already passes `str(user.id)`).

`decode_token` keeps the same contract: rejects a token missing `exp` (never
expires) or `sub`, rejects an expired or tampered token, and returns `None`
rather than raising. All existing `tests/test_security.py` and `tests/api/
test_auth.py` cases stay green (the test-only token-minting import moves from
`jose` to `jwt`).

## Consequences

- `ecdsa` (and the `python-jose` chain: `rsa`, `pyasn1`) leave the dependency
  tree; the PYSEC-2026-1325 finding is resolved rather than annotated. @severino
  re-runs `pip-audit` to confirm.
- One-line risk: PyJWT's `require`-options key differs from jose's; covered by the
  existing "reject token without exp / without sub" tests, so a regression fails
  CI immediately.
- No behavioural change for clients: token format, lifetime, and the 401-on-
  invalid flow are unchanged.
