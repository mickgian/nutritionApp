package com.meridia.shared.network

import com.meridia.shared.ApiConfig
import com.meridia.shared.auth.AuthManager
import com.meridia.shared.models.BoxDto
import com.meridia.shared.models.ErrorResponse
import com.meridia.shared.models.PlanDto
import com.meridia.shared.utils.ErrorHandler
import com.meridia.shared.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

interface BoxRepository {
    /** The active plan, or null when none is assigned (backend 404 → locked box). */
    suspend fun myPlan(): PlanDto?

    /** The weekly box for [week]; assumes a plan exists. */
    suspend fun box(week: Int): BoxDto
}

class BoxRepositoryImpl(
    private val client: HttpClient = HttpClientProvider.client,
    private val authManager: AuthManager? = null,
) : BoxRepository {

    private val base = ApiConfig.BASE_URL
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun myPlan(): PlanDto? = guarded {
        val response = client.get("$base/me/plan") { bearerAuth(requireToken()) }
        // No active plan is an expected "locked" condition, not an error.
        if (response.status == HttpStatusCode.NotFound) return@guarded null
        validated(response).body<PlanDto>()
    }

    override suspend fun box(week: Int): BoxDto = guarded {
        validated(
            client.get("$base/me/box") {
                bearerAuth(requireToken())
                parameter("week", week)
            },
        ).body<BoxDto>()
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
        Logger.authError("BOX_HTTP_ERROR", "status=${response.status.value}")
        throw BookingException(detailOrNull(body) ?: "Impossibile caricare il box. Riprova.")
    }

    private fun detailOrNull(body: String): String? =
        try {
            json.decodeFromString<ErrorResponse>(body).detail
        } catch (_: Exception) {
            null
        }
}
