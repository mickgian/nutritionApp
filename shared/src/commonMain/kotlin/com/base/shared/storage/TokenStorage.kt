package com.base.shared.storage

import com.base.shared.models.TokenWithExpiry

expect class TokenStorage(platformContext: Any?) {
    suspend fun save(token: TokenWithExpiry)
    suspend fun load(): TokenWithExpiry?
    suspend fun clear()
}