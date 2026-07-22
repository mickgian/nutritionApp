package com.base.shared.storage

import com.base.shared.models.TokenWithExpiry
import kotlinx.browser.localStorage
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

private const val ACCESS_TOKEN_KEY = "base_app_access_token"
private const val EXPIRES_AT_KEY = "base_app_expires_at"

actual class TokenStorage actual constructor(platformContext: Any?) {
    
    actual suspend fun save(token: TokenWithExpiry) {
        localStorage.setItem(ACCESS_TOKEN_KEY, token.accessToken)
        localStorage.setItem(EXPIRES_AT_KEY, token.expiresAt.epochSeconds.toString())
    }

    actual suspend fun load(): TokenWithExpiry? {
        val accessToken = localStorage.getItem(ACCESS_TOKEN_KEY)
        val expiresAtString = localStorage.getItem(EXPIRES_AT_KEY)
        
        return if (accessToken != null && expiresAtString != null) {
            try {
                val expiresAt = Instant.fromEpochSeconds(expiresAtString.toLong())
                TokenWithExpiry(
                    accessToken = accessToken,
                    expiresAt = expiresAt
                )
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    actual suspend fun clear() {
        localStorage.removeItem(ACCESS_TOKEN_KEY)
        localStorage.removeItem(EXPIRES_AT_KEY)
    }
}