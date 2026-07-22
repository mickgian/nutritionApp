package com.base.shared.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String
)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type")  val tokenType:  String,
    @SerialName("expires_at")  val expiresAt:  Instant
)

@Serializable
data class SessionResponse(
    @SerialName("session_id") val sessionId: String,
    val name: String,
    val token: TokenResponse
)

@Serializable
data class AuthResponse(
    val id: Int,
    val email: String,
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_at") val expiresAt: Instant
)

data class TokenWithExpiry(
    val accessToken: String,
    val expiresAt: Instant
)

@Serializable
data class ErrorResponse(
    val detail: String
)

@Serializable
data class ValidationErrorResponse(
    val detail: String,
    val errors: List<ValidationErrorDetail>? = null
)

@Serializable
data class ValidationErrorDetail(
    val field: String,
    val message: String
)

