package com.meridia.shared.network

import com.meridia.shared.ApiConfig
import com.meridia.shared.auth.AuthManager
import com.meridia.shared.models.AdminClientDto
import com.meridia.shared.models.AdminOrderDto
import com.meridia.shared.models.AdminSlotDto
import com.meridia.shared.models.AvailabilityUpsertBody
import com.meridia.shared.models.ErrorResponse
import com.meridia.shared.models.PlanAssignBody
import com.meridia.shared.models.PromotionBody
import com.meridia.shared.models.PromotionResultDto
import com.meridia.shared.utils.ErrorHandler
import com.meridia.shared.utils.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * Studio (admin) endpoints (DEV-080). Every call is admin-guarded server-side;
 * a cliente token gets 403, surfaced here as an Italian [BookingException]. Only
 * the studio screens use this repository (client gate is UX; the server enforces).
 */
interface AdminRepository {
    suspend fun clients(): List<AdminClientDto>

    suspend fun orders(): List<AdminOrderDto>

    suspend fun assignPlan(clientId: Int, name: String, dailyKcal: Int, weeks: Int)

    suspend fun sendPromotion(title: String, body: String, icon: String): PromotionResultDto

    suspend fun availability(fromDate: String, toDate: String): List<AdminSlotDto>

    suspend fun setAvailability(startsAt: String, durationMin: Int, isOpen: Boolean): AdminSlotDto
}

class AdminRepositoryImpl(
    private val client: HttpClient = HttpClientProvider.client,
    private val authManager: AuthManager? = null,
) : AdminRepository {

    private val base = ApiConfig.BASE_URL
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    override suspend fun clients(): List<AdminClientDto> = guarded {
        validated(client.get("$base/admin/clients") { bearerAuth(requireToken()) })
            .body<List<AdminClientDto>>()
    }

    override suspend fun orders(): List<AdminOrderDto> = guarded {
        validated(client.get("$base/admin/orders") { bearerAuth(requireToken()) })
            .body<List<AdminOrderDto>>()
    }

    override suspend fun assignPlan(clientId: Int, name: String, dailyKcal: Int, weeks: Int) {
        guarded {
            validated(
                client.post("$base/admin/clients/$clientId/plan") {
                    bearerAuth(requireToken())
                    contentType(ContentType.Application.Json)
                    setBody(PlanAssignBody(name = name, dailyKcal = dailyKcal, weeks = weeks))
                },
            )
        }
    }

    override suspend fun sendPromotion(
        title: String,
        body: String,
        icon: String,
    ): PromotionResultDto = guarded {
        validated(
            client.post("$base/admin/promotions") {
                bearerAuth(requireToken())
                contentType(ContentType.Application.Json)
                setBody(PromotionBody(title = title, body = body, icon = icon))
            },
        ).body<PromotionResultDto>()
    }

    override suspend fun availability(fromDate: String, toDate: String): List<AdminSlotDto> =
        guarded {
            validated(
                client.get("$base/admin/availability") {
                    bearerAuth(requireToken())
                    parameter("from", fromDate)
                    parameter("to", toDate)
                },
            ).body<List<AdminSlotDto>>()
        }

    override suspend fun setAvailability(
        startsAt: String,
        durationMin: Int,
        isOpen: Boolean,
    ): AdminSlotDto = guarded {
        validated(
            client.put("$base/admin/availability") {
                bearerAuth(requireToken())
                contentType(ContentType.Application.Json)
                setBody(
                    AvailabilityUpsertBody(
                        startsAt = startsAt,
                        durationMin = durationMin,
                        isOpen = isOpen,
                    ),
                )
            },
        ).body<AdminSlotDto>()
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
        Logger.authError("ADMIN_HTTP_ERROR", "status=${response.status.value}")
        throw BookingException(detailOrNull(body) ?: "Operazione non riuscita. Riprova.")
    }

    private fun detailOrNull(body: String): String? =
        try {
            json.decodeFromString<ErrorResponse>(body).detail
        } catch (_: Exception) {
            null
        }
}
