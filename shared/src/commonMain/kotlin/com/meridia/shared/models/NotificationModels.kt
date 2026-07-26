package com.meridia.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A behavior-profiled notification, from GET /api/v1/me/notifications. */
@Serializable
data class NotificationDto(
    val id: Int,
    val kind: String,
    val title: String,
    val body: String,
    val icon: String,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String,
)

/** The count returned by POST /api/v1/me/notifications/read. */
@Serializable
data class MarkReadDto(
    val updated: Int,
)
