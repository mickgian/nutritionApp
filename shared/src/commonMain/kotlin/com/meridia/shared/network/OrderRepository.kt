package com.meridia.shared.network

import com.meridia.shared.ApiConfig
import com.meridia.shared.auth.AuthManager
import com.meridia.shared.models.ErrorResponse
import com.meridia.shared.models.OrderCreateRequest
import com.meridia.shared.models.OrderDto
import com.meridia.shared.utils.ErrorHandler
import com.meridia.shared.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

interface OrderRepository {
    /** Places a box order; the backend requires an active plan (else 403). */
    suspend fun createOrder(formula: String, pickup: String): OrderDto

    /** The caller's own orders (most recent first). */
    suspend fun myOrders(): List<OrderDto>
}

class OrderRepositoryImpl(
    private val client: HttpClient = HttpClientProvider.client,
    private val authManager: AuthManager? = null,
) : OrderRepository {

    private val base = ApiConfig.BASE_URL
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun createOrder(formula: String, pickup: String): OrderDto = guarded {
        validated(
            client.post("$base/orders") {
                bearerAuth(requireToken())
                contentType(ContentType.Application.Json)
                setBody(OrderCreateRequest(formula = formula, pickup = pickup))
            },
        ).body<OrderDto>()
    }

    override suspend fun myOrders(): List<OrderDto> = guarded {
        validated(client.get("$base/orders") { bearerAuth(requireToken()) }).body<List<OrderDto>>()
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
        Logger.authError("ORDER_HTTP_ERROR", "status=${response.status.value}")
        throw BookingException(detailOrNull(body) ?: "Impossibile completare l'ordine. Riprova.")
    }

    private fun detailOrNull(body: String): String? =
        try {
            json.decodeFromString<ErrorResponse>(body).detail
        } catch (_: Exception) {
            null
        }
}
