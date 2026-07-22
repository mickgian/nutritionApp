package com.base.shared.auth

import com.base.shared.models.TokenWithExpiry
import com.base.shared.storage.TokenStorage
import com.base.shared.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val token: TokenWithExpiry) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthManager(
    private val tokenStorage: TokenStorage
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    suspend fun initialize() {
        Logger.authInfo("INITIALIZE_START", "Starting authentication manager initialization")
        try {
            val token = tokenStorage.load()
            if (token != null) {
                Logger.logToken("ACCESS", token.accessToken, "INITIALIZE_TOKEN_LOADED")
                val isValid = isTokenValid(token)
                Logger.authInfo("INITIALIZE_TOKEN_VALIDATION", "Token valid: $isValid, expires at: ${token.expiresAt}")
                
                if (isValid) {
                    _authState.value = AuthState.Authenticated(token)
                    Logger.authInfo("INITIALIZE_SUCCESS", "User authenticated with valid token")
                } else {
                    Logger.authInfo("INITIALIZE_TOKEN_EXPIRED", "Token expired, clearing storage")
                    tokenStorage.clear()
                    _authState.value = AuthState.Unauthenticated
                    Logger.authInfo("INITIALIZE_CLEARED", "Expired token cleared, user unauthenticated")
                }
            } else {
                Logger.authInfo("INITIALIZE_NO_TOKEN", "No stored token found")
                _authState.value = AuthState.Unauthenticated
            }
        } catch (e: Exception) {
            Logger.authError("INITIALIZE_FAILED", e.message ?: "Unknown error", e)
            _authState.value = AuthState.Error("Failed to load token: ${e.message}")
        }
    }

    suspend fun saveToken(token: TokenWithExpiry) {
        Logger.authInfo("SAVE_TOKEN_START", "Attempting to save authentication token")
        Logger.logToken("ACCESS", token.accessToken, "SAVE_TOKEN_RECEIVED")
        Logger.authInfo("SAVE_TOKEN_EXPIRY", "Token expires at: ${token.expiresAt}")
        
        try {
            tokenStorage.save(token)
            _authState.value = AuthState.Authenticated(token)
            Logger.authInfo("SAVE_TOKEN_SUCCESS", "Token successfully saved and user authenticated")
        } catch (e: Exception) {
            Logger.authError("SAVE_TOKEN_FAILED", e.message ?: "Unknown error", e)
            _authState.value = AuthState.Error("Failed to save token: ${e.message}")
        }
    }

    suspend fun logout() {
        Logger.authInfo("LOGOUT_START", "User initiated logout")
        try {
            tokenStorage.clear()
            _authState.value = AuthState.Unauthenticated
            Logger.authInfo("LOGOUT_SUCCESS", "Token cleared, user logged out successfully")
        } catch (e: Exception) {
            Logger.authError("LOGOUT_FAILED", e.message ?: "Unknown error", e)
            _authState.value = AuthState.Error("Failed to logout: ${e.message}")
        }
    }

    fun getCurrentToken(): String? {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> {
                if (isTokenValid(state.token)) {
                    state.token.accessToken
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun isTokenValid(token: TokenWithExpiry): Boolean {
        val now = Clock.System.now()
        return token.expiresAt > now
    }

    fun isAuthenticated(): Boolean {
        return when (val state = _authState.value) {
            is AuthState.Authenticated -> isTokenValid(state.token)
            else -> false
        }
    }

    suspend fun checkTokenAndRefresh(): Boolean {
        Logger.authDebug("CHECK_TOKEN_START", "Checking token validity")
        val currentState = _authState.value
        if (currentState is AuthState.Authenticated) {
            val isValid = isTokenValid(currentState.token)
            Logger.authDebug("CHECK_TOKEN_VALIDATION", "Token valid: $isValid")
            
            if (!isValid) {
                Logger.authInfo("CHECK_TOKEN_EXPIRED", "Token expired, clearing and logging out")
                tokenStorage.clear()
                _authState.value = AuthState.Unauthenticated
                return false
            }
            Logger.authDebug("CHECK_TOKEN_VALID", "Token is still valid")
            return true
        }
        Logger.authDebug("CHECK_TOKEN_NOT_AUTHENTICATED", "User not authenticated")
        return false
    }
}