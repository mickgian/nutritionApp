package com.meridia.shared.network.auth

import com.meridia.shared.ApiConfig
import com.meridia.shared.auth.AuthManager
import com.meridia.shared.models.ErrorResponse
import com.meridia.shared.models.LoginRequest
import com.meridia.shared.models.RegisterRequest
import com.meridia.shared.models.TokenResponse
import com.meridia.shared.models.TokenWithExpiry
import com.meridia.shared.models.UserResponse
import com.meridia.shared.network.HttpClientProvider
import com.meridia.shared.utils.ErrorHandler
import com.meridia.shared.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes

/** Raised for auth failures; the message is already Italian and user-facing. */
class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface AuthRepository {
    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        privacyConsent: Boolean,
    ): UserResponse
    suspend fun login(email: String, password: String): TokenResponse
    suspend fun loginAndSaveToken(email: String, password: String): TokenResponse
    suspend fun me(): UserResponse
}

class AuthRepositoryImpl(
    private val client: HttpClient = HttpClientProvider.client,
    private val authManager: AuthManager? = null,
) : AuthRepository {

    private val base = ApiConfig.BASE_URL
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun register(
        email: String,
        password: String,
        fullName: String,
        privacyConsent: Boolean,
    ): UserResponse {
        Logger.authInfo("REGISTER_START", "Registering $email")
        try {
            val resp = client.post("$base/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(
                    RegisterRequest(
                        email = email,
                        password = password,
                        fullName = fullName,
                        privacyConsent = privacyConsent,
                    ),
                )
            }
            if (resp.status.isSuccess()) return resp.body<UserResponse>()

            val body = resp.bodyAsText()
            val message = when (resp.status.value) {
                400 -> detailOrNull(body) ?: "Registrazione non riuscita. Riprova."
                409 -> detailOrNull(body) ?: "Email già registrata"
                422 -> "Controlla i dati inseriti e riprova."
                else -> detailOrNull(body) ?: "Registrazione non riuscita. Riprova."
            }
            Logger.authError("REGISTER_HTTP_ERROR", "status=${resp.status.value}")
            throw AuthException(message)
        } catch (e: AuthException) {
            throw e
        } catch (e: Exception) {
            val friendly = ErrorHandler.handleHttpError(e)
            Logger.authError("REGISTER_FAILED", friendly, e)
            throw AuthException(friendly, e)
        }
    }

    override suspend fun login(email: String, password: String): TokenResponse {
        Logger.authInfo("LOGIN_START", "Login $email")
        try {
            val resp = client.post("$base/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email = email, password = password))
            }
            if (resp.status.isSuccess()) return resp.body<TokenResponse>()

            val body = resp.bodyAsText()
            val message = when (resp.status.value) {
                401 -> detailOrNull(body) ?: "Credenziali non valide"
                403 -> detailOrNull(body) ?: "Account disattivato"
                422 -> "Controlla i dati inseriti e riprova."
                else -> detailOrNull(body) ?: "Accesso non riuscito. Riprova."
            }
            Logger.authError("LOGIN_HTTP_ERROR", "status=${resp.status.value}")
            throw AuthException(message)
        } catch (e: AuthException) {
            throw e
        } catch (e: Exception) {
            val friendly = ErrorHandler.handleHttpError(e)
            Logger.authError("LOGIN_FAILED", friendly, e)
            throw AuthException(friendly, e)
        }
    }

    override suspend fun loginAndSaveToken(email: String, password: String): TokenResponse {
        val token = login(email, password)
        authManager?.saveToken(
            TokenWithExpiry(
                accessToken = token.accessToken,
                expiresAt = Clock.System.now() + ACCESS_TOKEN_LIFETIME,
            ),
        )
        return token
    }

    override suspend fun me(): UserResponse {
        val token = authManager?.getCurrentToken()
            ?: throw AuthException(SESSION_EXPIRED)
        val resp = client.get("$base/auth/me") { bearerAuth(token) }
        if (resp.status == HttpStatusCode.Unauthorized) {
            authManager?.logout()
            throw AuthException(SESSION_EXPIRED)
        }
        if (!resp.status.isSuccess()) {
            throw AuthException("Impossibile recuperare il profilo. Riprova.")
        }
        return resp.body<UserResponse>()
    }

    /** Extracts the backend's Italian `detail` string, or null for non-string bodies (e.g. 422). */
    private fun detailOrNull(body: String): String? =
        try {
            json.decodeFromString<ErrorResponse>(body).detail
        } catch (_: Exception) {
            null
        }

    companion object {
        private val ACCESS_TOKEN_LIFETIME = 60.minutes
        private const val SESSION_EXPIRED = "Sessione scaduta. Effettua di nuovo l'accesso."
    }
}
