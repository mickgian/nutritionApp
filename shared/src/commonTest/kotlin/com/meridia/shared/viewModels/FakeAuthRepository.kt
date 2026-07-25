package com.meridia.shared.viewModels

import com.meridia.shared.models.TokenResponse
import com.meridia.shared.models.UserResponse
import com.meridia.shared.network.auth.AuthRepository

/**
 * In-memory [AuthRepository] for ViewModel tests. Either returns a canned result
 * or throws the supplied error, so success and failure paths can be driven
 * without a network.
 */
class FakeAuthRepository(
    private val loginResult: TokenResponse = TokenResponse(accessToken = "test-token", tokenType = "bearer"),
    private val loginError: Throwable? = null,
    private val registerResult: UserResponse = UserResponse(1, "mario@example.com", "Mario Rossi", "cliente"),
    private val registerError: Throwable? = null,
) : AuthRepository {

    var lastLoginEmail: String? = null
    var lastRegisterFullName: String? = null

    override suspend fun register(email: String, password: String, fullName: String): UserResponse {
        lastRegisterFullName = fullName
        registerError?.let { throw it }
        return registerResult
    }

    override suspend fun login(email: String, password: String): TokenResponse {
        lastLoginEmail = email
        loginError?.let { throw it }
        return loginResult
    }

    override suspend fun loginAndSaveToken(email: String, password: String): TokenResponse =
        login(email, password)

    override suspend fun me(): UserResponse = registerResult
}
