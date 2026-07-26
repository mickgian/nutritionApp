package com.meridia.shared.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Payload to place a box order (POST /api/v1/orders). */
@Serializable
data class OrderCreateRequest(
    val formula: String,
    val pickup: String,
)

/** A placed box order, from POST/GET /api/v1/orders. Price is server-authoritative cents. */
@Serializable
data class OrderDto(
    val id: Int,
    val formula: String,
    val pickup: String,
    val status: String,
    @SerialName("price_cents") val priceCents: Int,
    @SerialName("created_at") val createdAt: String,
)
