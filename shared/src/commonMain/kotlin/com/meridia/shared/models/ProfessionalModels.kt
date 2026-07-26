package com.meridia.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A review on the professional, from GET /api/v1/professional. */
@Serializable
data class ReviewDto(
    val id: Int,
    @SerialName("author_name") val authorName: String,
    val rating: Int,
    val body: String,
    @SerialName("created_at") val createdAt: String,
)

/** The studio nutritionist profile + reviews, from GET /api/v1/professional. */
@Serializable
data class ProfessionalDto(
    val id: Int,
    @SerialName("full_name") val fullName: String,
    val title: String,
    val bio: String,
    val rating: Double,
    @SerialName("reviews_count") val reviewsCount: Int,
    val reviews: List<ReviewDto> = emptyList(),
)
