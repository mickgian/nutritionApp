package com.meridia.shared.models

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST /api/v1/auth/register body. */
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("privacy_consent") val privacyConsent: Boolean,
)

/** POST /api/v1/auth/login body. */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

/** POST /api/v1/auth/login response. */
@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
)

/** Public user shape returned by /auth/register and /auth/me. */
@Serializable
data class UserResponse(
    val id: Int,
    val email: String,
    @SerialName("full_name") val fullName: String,
    val role: String,
)

/**
 * Locally-stored access token plus a client-side expiry hint. The backend login
 * response carries only the token; expiry is set client-side (see
 * AuthRepositoryImpl) and the server stays authoritative — a 401 on any
 * authorized call triggers re-login.
 */
data class TokenWithExpiry(
    val accessToken: String,
    val expiresAt: Instant,
)

@Serializable
data class ErrorResponse(
    val detail: String,
)
