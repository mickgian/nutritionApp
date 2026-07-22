package com.base.shared.network.auth

import com.base.shared.ApiConfig
import com.base.shared.models.SessionResponse
import com.base.shared.network.HttpClientProvider
import com.base.shared.utils.NetworkLogger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

interface SessionRepository {
    suspend fun createSession(): SessionResponse
    suspend fun listSessions(): List<SessionResponse>
    suspend fun renameSession(sessionId: String, newName: String): SessionResponse
    suspend fun deleteSession(sessionId: String)
}

/**
 * @param token – JWT access token returned by /auth/login
 */
class SessionRepositoryImpl(
    private val token: String,
    private val client: HttpClient = HttpClientProvider.client
) : SessionRepository {

    /* --- endpoints ------------------------------------------------------ */
    // POST /auth/session   (create)
    // GET  /auth/sessions  (list)
    // PATCH|DELETE /auth/session/{id}/...
    private val sessionBase = "${ApiConfig.BASE_URL}/auth/session"
    private val sessionsUrl = "${ApiConfig.BASE_URL}/auth/sessions"

    /** Helper to add bearer to every call */
    private fun HttpRequestBuilder.auth() {
        header(HttpHeaders.Authorization, "Bearer $token")
    }

    override suspend fun createSession(): SessionResponse {
        val headers = mapOf(
            "Authorization" to "Bearer ${token.take(10)}...",
            "Content-Type" to "application/json"
        )
        
        NetworkLogger.logRequest("POST", sessionBase, headers, tag = "SessionRepo")
        
        return try {
            val response = client.post(sessionBase) {
                auth()
                contentType(ContentType.Application.Json)
            }
            
            val sessionResponse: SessionResponse = response.body()
            NetworkLogger.logResponse("POST", sessionBase, response.status.value, 
                "Created session: ${sessionResponse.sessionId}", "SessionRepo")
            
            sessionResponse
        } catch (e: Exception) {
            NetworkLogger.logError("POST", sessionBase, e.message ?: "Unknown error", "SessionRepo")
            throw e
        }
    }

    override suspend fun listSessions(): List<SessionResponse> {
        val headers = mapOf(
            "Authorization" to "Bearer ${token.take(10)}..."
        )
        
        NetworkLogger.logRequest("GET", sessionsUrl, headers, tag = "SessionRepo")
        
        return try {
            val response = client.get(sessionsUrl) {
                auth()
            }
            
            val sessions: List<SessionResponse> = response.body()
            
            // Log detailed session information
            NetworkLogger.logResponse("GET", sessionsUrl, response.status.value, 
                "Found ${sessions.size} sessions", "SessionRepo")
            NetworkLogger.logSessionCount(sessions.size, "SessionRepo")
            
            if (sessions.isNotEmpty()) {
                NetworkLogger.logSessionDetails(sessions.map { session ->
                    "Session(id=${session.sessionId.take(8)}..., name='${session.name}')"
                }, "SessionRepo")
            }
            
            sessions
        } catch (e: Exception) {
            NetworkLogger.logError("GET", sessionsUrl, e.message ?: "Unknown error", "SessionRepo")
            throw e
        }
    }

    override suspend fun renameSession(sessionId: String, newName: String): SessionResponse {
        val url = "$sessionBase/$sessionId/name"
        val headers = mapOf(
            "Authorization" to "Bearer ${token.take(10)}...",
            "Content-Type" to "application/x-www-form-urlencoded"
        )
        val body = "name=$newName"
        
        NetworkLogger.logRequest("PATCH", url, headers, body, "SessionRepo")
        
        return try {
            val response = client.patch(url) {
                auth()
                setBody(
                    FormDataContent(Parameters.build { append("name", newName) })
                )
            }
            
            // Debug: Log the raw response body first
            val rawBody = response.bodyAsText()
            println("KMP_LOG: [DEBUG] Raw rename response (status ${response.status.value}): $rawBody")
            
            // Check if response was successful
            if (response.status.value in 200..299) {
                // Parse the raw JSON manually to create SessionResponse
                val sessionResponse = Json.decodeFromString<SessionResponse>(rawBody)
                NetworkLogger.logResponse("PATCH", url, response.status.value, 
                    "Renamed session ${sessionId.take(8)}... to '$newName'", "SessionRepo")
                sessionResponse
            } else {
                // Handle error response
                val errorMessage = try {
                    val errorJson = Json.parseToJsonElement(rawBody)
                    errorJson.jsonObject["detail"]?.jsonPrimitive?.content ?: "Unknown error"
                } catch (e: Exception) {
                    "Failed to parse error response: $rawBody"
                }
                throw Exception("API Error ${response.status.value}: $errorMessage")
            }
        } catch (e: Exception) {
            NetworkLogger.logError("PATCH", url, e.message ?: "Unknown error", "SessionRepo")
            throw e
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        val url = "$sessionBase/$sessionId"
        val headers = mapOf(
            "Authorization" to "Bearer ${token.take(10)}..."
        )
        
        NetworkLogger.logRequest("DELETE", url, headers, tag = "SessionRepo")
        
        try {
            val response = client.delete(url) { auth() }
            
            NetworkLogger.logResponse("DELETE", url, response.status.value, 
                "Deleted session ${sessionId.take(8)}...", "SessionRepo")
        } catch (e: Exception) {
            NetworkLogger.logError("DELETE", url, e.message ?: "Unknown error", "SessionRepo")
            throw e
        }
    }
}
