package com.base.shared.network.chat

import com.base.shared.ApiConfig
import com.base.shared.models.ChatMessage
import com.base.shared.network.HttpClientProvider
import com.base.shared.utils.NetworkLogger
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json

interface ChatRepository {
    /**
     * Send one user message to the backend and return **only** the assistant's
     * reply text (the view-model converts it to a ChatMessage later).
     *
     * @param sessionId – ID of the current chat session (ignored by the
     *                   default impl because the backend knows it from the JWT,
     *                   but keeping the parameter lets us pass it to any
     *                   alternative implementation that might need it).
     * @param userText  – the message the user typed.
     */
    suspend fun sendMessage(sessionId: String, userText: String): String
    
    /**
     * Load existing chat history for a session from the backend.
     *
     * @param sessionId – ID of the chat session to load history for
     * @return List of ChatMessage objects representing the conversation history
     */
    suspend fun loadChatHistory(sessionId: String): List<ChatMessage>
}

class ChatRepositoryImpl(
    private val jwt: String,
    private val client: HttpClient = HttpClientProvider.client
) : ChatRepository {

    private val chatUrl = "${ApiConfig.BASE_URL}/chatbot/chat"
    private val messagesUrl = "${ApiConfig.BASE_URL}/chatbot/messages"

    /** POST /api/v1/chatbot/chat  */
    override suspend fun sendMessage(sessionId: String, userText: String): String {
        val requestBody = ChatCompletionRequestDto(
            messages = listOf(ChatRoleMessageDto(role = "user", content = userText))
        )

        // Log the request details
        NetworkLogger.logRequest(
            method = "POST",
            url = chatUrl,
            headers = mapOf("Authorization" to "Bearer ${jwt.take(10)}..."), 
            body = Json.encodeToString(ChatCompletionRequestDto.serializer(), requestBody),
            tag = "ChatRepo"
        )

        try {
            val httpResponse: HttpResponse = client.post(chatUrl) {
                header(HttpHeaders.Authorization, "Bearer $jwt")
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            // Get response body once
            val responseBody = httpResponse.bodyAsText()
            
            // Log successful response
            NetworkLogger.logResponse(
                method = "POST",
                url = chatUrl,
                statusCode = httpResponse.status.value,
                body = responseBody,
                tag = "ChatRepo"
            )

            // Parse the response
            val resp: ChatCompletionResponseDto = Json.decodeFromString(ChatCompletionResponseDto.serializer(), responseBody)
            
            // Find the LATEST assistant message (the new response), not the first one
            // The backend returns the full conversation history, we need the most recent assistant reply
            val latestAssistantMessage = resp.messages
                .filter { it.role == "assistant" }
                .lastOrNull() // Get the last assistant message, which is the new response
            
            return latestAssistantMessage?.content ?: "(no reply)"
        } catch (e: Exception) {
            NetworkLogger.logError(
                method = "POST",
                url = chatUrl,
                error = e.message ?: "Unknown error",
                tag = "ChatRepo"
            )
            throw e
        }
    }

    /** GET /api/v1/chatbot/messages */
    override suspend fun loadChatHistory(sessionId: String): List<ChatMessage> {
        // Log the request details
        NetworkLogger.logRequest(
            method = "GET",
            url = messagesUrl,
            headers = mapOf("Authorization" to "Bearer ${jwt.take(10)}..."),
            body = null,
            tag = "ChatRepo"
        )

        try {
            val httpResponse: HttpResponse = client.get(messagesUrl) {
                header(HttpHeaders.Authorization, "Bearer $jwt")
            }

            // Get response body once
            val responseBody = httpResponse.bodyAsText()
            
            // Log successful response
            NetworkLogger.logResponse(
                method = "GET",
                url = messagesUrl,
                statusCode = httpResponse.status.value,
                body = responseBody,
                tag = "ChatRepo"
            )

            // Parse the response
            val resp: ChatCompletionResponseDto = Json.decodeFromString(ChatCompletionResponseDto.serializer(), responseBody)
            
            // Convert backend messages to ChatMessage objects
            return resp.messages.map { message ->
                ChatMessage(
                    text = message.content,
                    fromUser = message.role == "user"
                )
            }
        } catch (e: Exception) {
            NetworkLogger.logError(
                method = "GET",
                url = messagesUrl,
                error = e.message ?: "Unknown error",
                tag = "ChatRepo"
            )
            throw e
        }
    }
}