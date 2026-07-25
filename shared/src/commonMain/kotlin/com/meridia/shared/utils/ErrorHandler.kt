package com.meridia.shared.utils

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException

/**
 * Maps throwables to Italian, user-facing messages. Repository code already
 * raises Italian AuthExceptions for HTTP errors it understands; this handler
 * covers network failures and any unmapped HTTP status.
 */
object ErrorHandler {

    fun handleHttpError(exception: Throwable): String = when (exception) {
        is ClientRequestException -> clientMessage(exception.response.status.value)
        is ServerResponseException -> SERVER_ERROR
        is ResponseException -> clientMessage(exception.response.status.value)
        else -> networkMessage(exception.message)
    }

    private fun clientMessage(status: Int): String = when (status) {
        400 -> "Richiesta non valida. Controlla i dati inseriti."
        401 -> "Credenziali non valide."
        403 -> "Accesso non consentito."
        404 -> "Risorsa non trovata."
        409 -> "La risorsa esiste già."
        422 -> "Controlla i dati inseriti e riprova."
        429 -> "Troppe richieste. Riprova più tardi."
        else -> "Richiesta non riuscita. Riprova."
    }

    private fun networkMessage(raw: String?): String {
        val message = raw?.lowercase() ?: ""
        return when {
            message.contains("connect") || message.contains("unreachable") ||
                message.contains("host") || message.contains("network") ->
                "Impossibile contattare il server. Controlla la connessione."
            message.contains("timeout") -> "Tempo scaduto. Riprova."
            raw.isNullOrBlank() -> "Si è verificato un errore imprevisto."
            // A repository AuthException carries an already-Italian message: pass it through.
            else -> raw
        }
    }

    private const val SERVER_ERROR = "Errore del server. Riprova più tardi."
}
