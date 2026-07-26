package com.meridia.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** An open consultation slot from GET /api/v1/availability. */
@Serializable
data class SlotDto(
    val id: Int,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("duration_min") val durationMin: Int,
)

/** POST /api/v1/appointments body. */
@Serializable
data class BookAppointmentRequest(
    @SerialName("slot_id") val slotId: Int,
    @SerialName("visit_type") val visitType: String,
)

/** An appointment from POST/GET /api/v1/appointments. */
@Serializable
data class AppointmentDto(
    val id: Int,
    @SerialName("slot_id") val slotId: Int?,
    @SerialName("visit_type") val visitType: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    val status: String,
    @SerialName("price_cents") val priceCents: Int,
)

/**
 * Client-side visit types. Prices are indicative (the server is authoritative and
 * returns the real amount on the created appointment); the wire value maps to the
 * backend enum.
 */
enum class VisitTypeUi(val wire: String, val label: String, val priceCents: Int) {
    Prima("prima", "Prima visita", 9000),
    Controllo("controllo", "Visita di controllo", 5000),
}
