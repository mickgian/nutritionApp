package com.meridia.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** An active store credit, from GET /api/v1/me/credits. Amount is in euro cents. */
@Serializable
data class CreditDto(
    val id: Int,
    @SerialName("amount_cents") val amountCents: Int,
    val reason: String,
    @SerialName("source_appointment_id") val sourceAppointmentId: Int? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String,
)

/** The result of DELETE /api/v1/appointments/{id}: status + any credit issued. */
@Serializable
data class CancellationDto(
    @SerialName("appointment_id") val appointmentId: Int,
    val status: String,
    val credit: CreditDto? = null,
)
