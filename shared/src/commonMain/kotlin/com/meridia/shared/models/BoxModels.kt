package com.meridia.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The client's active plan, from GET /api/v1/me/plan (404 → no plan → locked box). */
@Serializable
data class PlanDto(
    val id: Int,
    val name: String,
    @SerialName("daily_kcal") val dailyKcal: Int,
    val weeks: Int,
    @SerialName("is_active") val isActive: Boolean = true,
)

/** A catalog meal (kcal + macros + handling notes), from the box or GET /api/v1/meals/{id}. */
@Serializable
data class MealDto(
    val id: Int,
    val title: String,
    val slot: String,
    val kcal: Int,
    @SerialName("protein_g") val proteinG: Int,
    @SerialName("carb_g") val carbG: Int,
    @SerialName("fat_g") val fatG: Int,
    @SerialName("asset_ref") val assetRef: String? = null,
    @SerialName("storage_note") val storageNote: String? = null,
    @SerialName("reheat_note") val reheatNote: String? = null,
)

/** One (weekday, slot) cell of the weekly box. */
@Serializable
data class BoxItemDto(
    val weekday: Int,
    val slot: String,
    val meal: MealDto,
)

/** The weekly 14-meal box, from GET /api/v1/me/box?week=. */
@Serializable
data class BoxDto(
    @SerialName("week_index") val weekIndex: Int,
    val items: List<BoxItemDto> = emptyList(),
)
