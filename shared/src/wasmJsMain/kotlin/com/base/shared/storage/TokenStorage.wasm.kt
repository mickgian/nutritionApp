package com.base.shared.storage

import com.base.shared.models.TokenWithExpiry
import kotlinx.datetime.Instant

private const val ACCESS_TOKEN_KEY = "base_app_access_token"
private const val EXPIRES_AT_KEY = "base_app_expires_at"

// Simple in-memory storage for WASM (localStorage is not available in WASM)
private var memoryStorage = mutableMapOf<String, String>()

actual class TokenStorage actual constructor(platformContext: Any?) {
    
    actual suspend fun save(token: TokenWithExpiry) {
        memoryStorage[ACCESS_TOKEN_KEY] = token.accessToken
        memoryStorage[EXPIRES_AT_KEY] = token.expiresAt.epochSeconds.toString()
    }

    actual suspend fun load(): TokenWithExpiry? {
        val accessToken = memoryStorage[ACCESS_TOKEN_KEY]
        val expiresAtString = memoryStorage[EXPIRES_AT_KEY]
        
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
        memoryStorage.remove(ACCESS_TOKEN_KEY)
        memoryStorage.remove(EXPIRES_AT_KEY)
    }
}