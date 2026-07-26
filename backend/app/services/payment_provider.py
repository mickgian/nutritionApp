"""Payment provider abstraction (ADR-002).

A :class:`PaymentProvider` hides the PSP behind ``charge(token, amount)``. The
backend passes only an opaque device/PSP token — never a card number — and keeps
the provider's charge reference. Ship the mock; a real PSP implements the same
protocol with no change to callers.
"""

from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class PaymentResult:
    success: bool
    reference: str  # the provider's charge reference (never a PAN)


class PaymentProvider(Protocol):
    name: str

    def charge(self, token: str, amount_cents: int) -> PaymentResult: ...


# Sentinel token that makes the mock provider decline, for exercising the
# failure path in tests and the client.
FAIL_TOKEN = "tok_fail"


class MockPaymentProvider:
    """Deterministic provider: succeeds for any token except :data:`FAIL_TOKEN`."""

    name = "mock"

    def charge(self, token: str, amount_cents: int) -> PaymentResult:
        if token == FAIL_TOKEN:
            return PaymentResult(success=False, reference=f"ch_mock_declined_{amount_cents}")
        return PaymentResult(success=True, reference=f"ch_mock_{abs(hash(token)) % 10_000_000}")
