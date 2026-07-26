package com.meridia.shared.network

import com.meridia.shared.ApiConfig
import com.meridia.shared.auth.AuthManager
import com.meridia.shared.models.AppointmentDto
import com.meridia.shared.models.BookAppointmentRequest
import com.meridia.shared.models.ErrorResponse
import com.meridia.shared.models.SlotDto
import com.meridia.shared.utils.ErrorHandler
import com.meridia.shared.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/** Raised for booking failures; the message is already Italian and user-facing. */
class BookingException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface AppointmentRepository {
    suspend fun availability(fromDate: String, toDate: String): List<SlotDto>
    suspend fun book(slotId: Int, visitType: String): AppointmentDto
    suspend fun myAppointments(): List<AppointmentDto>
}

class AppointmentRepositoryImpl(
    private val client: HttpClient = HttpClientProvider.client,
    private val authManager: AuthManager? = null,
) : AppointmentRepository {

    private val base = ApiConfig.BASE_URL
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun availability(fromDate: String, toDate: String): List<SlotDto> = guarded {
        validated(
            client.get("$base/availability") {
                bearerAuth(requireToken())
                parameter("from", fromDate)
                parameter("to", toDate)
            },
        ).body<List<SlotDto>>()
    }

    override suspend fun book(slotId: Int, visitType: String): AppointmentDto = guarded {
        validated(
            client.post("$base/appointments") {
                bearerAuth(requireToken())
                contentType(ContentType.Application.Json)
                setBody(BookAppointmentRequest(slotId = slotId, visitType = visitType))
            },
        ).body<AppointmentDto>()
    }

    override suspend fun myAppointments(): List<AppointmentDto> = guarded {
        validated(client.get("$base/appointments") { bearerAuth(requireToken()) })
            .body<List<AppointmentDto>>()
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
        val message = detailOrNull(body) ?: defaultMessage(response.status.value)
        Logger.authError("BOOKING_HTTP_ERROR", "status=${response.status.value}")
        throw BookingException(message)
    }

    private fun defaultMessage(status: Int): String = when (status) {
        404 -> "Orario non trovato"
        409 -> "Orario non più disponibile"
        else -> "Operazione non riuscita. Riprova."
    }

    private fun detailOrNull(body: String): String? =
        try {
            json.decodeFromString<ErrorResponse>(body).detail
        } catch (_: Exception) {
            null
        }
}
