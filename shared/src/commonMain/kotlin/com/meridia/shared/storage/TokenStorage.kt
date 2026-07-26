package com.meridia.shared.storage

import com.meridia.shared.models.TokenWithExpiry

expect class TokenStorage(platformContext: Any?) {
    suspend fun save(token: TokenWithExpiry)
    suspend fun load(): TokenWithExpiry?
    suspend fun clear()
}