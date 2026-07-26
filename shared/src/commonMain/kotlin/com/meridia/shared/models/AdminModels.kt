package com.meridia.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A client row for the studio directory (GET /api/v1/admin/clients). */
@Serializable
data class AdminClientDto(
    val id: Int,
    @SerialName("full_name") val fullName: String,
    val email: String,
    @SerialName("active_plan") val activePlan: String? = null,
)

/** A box order across all clients (GET /api/v1/admin/orders). Price is server cents. */
@Serializable
data class AdminOrderDto(
    val id: Int,
    @SerialName("client_id") val clientId: Int,
    @SerialName("client_name") val clientName: String,
    @SerialName("client_email") val clientEmail: String,
    val formula: String,
    val pickup: String,
    val status: String,
    @SerialName("price_cents") val priceCents: Int,
    @SerialName("created_at") val createdAt: String,
)

/** An availability slot in the studio calendar (GET/PUT /api/v1/admin/availability). */
@Serializable
data class AdminSlotDto(
    val id: Int,
    @SerialName("starts_at") val startsAt: String,
    @SerialName("duration_min") val durationMin: Int,
    @SerialName("is_open") val isOpen: Boolean,
)

/** Open or close a slot (PUT /api/v1/admin/availability). */
@Serializable
data class AvailabilityUpsertBody(
    @SerialName("starts_at") val startsAt: String,
    @SerialName("duration_min") val durationMin: Int = 60,
    @SerialName("is_open") val isOpen: Boolean,
)

/** Assign a plan to a client (POST /api/v1/admin/clients/{id}/plan). */
@Serializable
data class PlanAssignBody(
    val name: String,
    @SerialName("daily_kcal") val dailyKcal: Int,
    val weeks: Int,
)

/** Broadcast a promotion to every client (POST /api/v1/admin/promotions). */
@Serializable
data class PromotionBody(
    val title: String,
    val body: String,
    val icon: String = "📣",
)

/** How many clients received the broadcast (promotions response). */
@Serializable
data class PromotionResultDto(
    val recipients: Int,
)
