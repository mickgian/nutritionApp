package com.base.shared.storage

import com.base.shared.models.TokenWithExpiry
import kotlinx.datetime.Instant
import platform.Foundation.NSUserDefaults

private const val ACCESS_TOKEN_KEY = "base_app_access_token"
private const val EXPIRES_AT_KEY = "base_app_expires_at"

actual class TokenStorage actual constructor(platformContext: Any?) {
    
    actual suspend fun save(token: TokenWithExpiry) {
        NSUserDefaults.standardUserDefaults.setObject(token.accessToken, ACCESS_TOKEN_KEY)
        NSUserDefaults.standardUserDefaults.setDouble(
            token.expiresAt.epochSeconds.toDouble(),
            EXPIRES_AT_KEY
        )
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual suspend fun load(): TokenWithExpiry? {
        val accessToken = NSUserDefaults.standardUserDefaults.stringForKey(ACCESS_TOKEN_KEY)
        val expiresAt = NSUserDefaults.standardUserDefaults.doubleForKey(EXPIRES_AT_KEY)
        
        return if (accessToken != null && expiresAt > 0) {
            TokenWithExpiry(
                accessToken = accessToken,
                expiresAt = Instant.fromEpochSeconds(expiresAt.toLong())
            )
        } else {
            null
        }
    }

    actual suspend fun clear() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ACCESS_TOKEN_KEY)
        NSUserDefaults.standardUserDefaults.removeObjectForKey(EXPIRES_AT_KEY)
        NSUserDefaults.standardUserDefaults.synchronize()
    }
}