package com.meridia.shared.network

import com.meridia.shared.ApiConfig
import com.meridia.shared.auth.AuthManager
import com.meridia.shared.models.ErrorResponse
import com.meridia.shared.utils.ErrorHandler
import com.meridia.shared.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/** GDPR self-service (DEV-093): export or erase the caller's own data. */
interface PrivacyRepository {
    /** Right to portability — the caller's full personal-data document as raw JSON. */
    suspend fun exportData(): String

    /** Right to erasure — delete the caller's data and anonymise the account. */
    suspend fun deleteAccount()
}

class PrivacyRepositoryImpl(
    private val client: HttpClient = HttpClientProvider.client,
    private val authManager: AuthManager? = null,
) : PrivacyRepository {

    private val base = ApiConfig.BASE_URL
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun exportData(): String = guarded {
        validated(client.get("$base/me/export") { bearerAuth(requireToken()) }).bodyAsText()
    }

    override suspend fun deleteAccount(): Unit = guarded {
        validated(client.delete("$base/me") { bearerAuth(requireToken()) })
        Unit
    }

    private fun requireToken(): String =
        authManager?.getCurrentToken()
            ?: throw BookingException("Sessione scaduta. Effettua di nuovo l'accesso.")

    /** Wraps a call, mapping unexpected throwables to an Italian [BookingException]. */
    private suspend fun <T> guarded(block: suspend () -> T): T =
        try {
            block()
        } catch (e: BookingException) {
            throw e
        } catch (e: Exception) {
            throw BookingException(ErrorHandler.handleHttpError(e), e)
        }

    /** Returns the response on 2xx, else raises an Italian [BookingException]. */
    private suspend fun validated(response: HttpResponse): HttpResponse {
        if (response.status.isSuccess()) return response
        if (response.status == HttpStatusCode.Unauthorized) {
            authManager?.logout()
            throw BookingException("Sessione scaduta. Effettua di nuovo l'accesso.")
        }
        val body = response.bodyAsText()
        Logger.authError("PRIVACY_HTTP_ERROR", "status=${response.status.value}")
        throw BookingException(detailOrNull(body) ?: "Operazione non riuscita. Riprova.")
    }

    private fun detailOrNull(body: String): String? =
        try {
            json.decodeFromString<ErrorResponse>(body).detail
        } catch (_: Exception) {
            null
        }
}
