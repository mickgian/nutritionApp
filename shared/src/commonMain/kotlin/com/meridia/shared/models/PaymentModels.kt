package com.meridia.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A mock device/PSP payment token. A real integration replaces this with a
 * per-transaction token from Apple/Google Pay or a card element — never a PAN.
 */
const val MOCK_PAYMENT_TOKEN = "tok_ui"

/** POST /api/v1/payments body. Amount is server-authoritative (not sent). */
@Serializable
data class PaymentRequest(
    val target: String,
    @SerialName("target_id") val targetId: Int,
    val method: String,
    val token: String,
)

/** A recorded payment, from POST /api/v1/payments. */
@Serializable
data class PaymentDto(
    val id: Int,
    @SerialName("amount_cents") val amountCents: Int,
    val method: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
)

/** The three payment methods offered in the demo. */
enum class PaymentMethodUi(val wire: String, val label: String) {
    ApplePay("apple_pay", "Apple Pay"),
    GooglePay("google_pay", "Google Pay"),
    Card("card", "Carta di credito"),
}
