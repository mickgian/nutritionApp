package com.base.shared.utils

/**
 * Centralized logging prefixes for consistent log categorization across the app.
 * 
 * Usage examples:
 * - Logger.info("${LogPrefixes.NETWORK_REQUEST} GET /auth/sessions", "SessionRepo")
 * - Logger.info("${LogPrefixes.NETWORK_RESPONSE} 200 OK - Found 3 sessions", "SessionRepo")
 */
object LogPrefixes {
    
    // Network logging prefixes
    const val NETWORK_REQUEST = "[NETWORK-REQ]"
    const val NETWORK_RESPONSE = "[NETWORK-RES]"
    
    // Authentication logging
    const val AUTH_REQUEST = "[AUTH-REQ]"
    const val AUTH_RESPONSE = "[AUTH-RES]"
    
    // Chat logging
    const val CHAT_REQUEST = "[CHAT-REQ]"
    const val CHAT_RESPONSE = "[CHAT-RES]"
    
    // Session management logging
    const val SESSION_REQUEST = "[SESSION-REQ]"
    const val SESSION_RESPONSE = "[SESSION-RES]"
    
    // Generic network logging for any API call
    const val API_REQUEST = "[API-REQ]"
    const val API_RESPONSE = "[API-RES]"
    
    // UI State logging
    const val UI_STATE = "[UI-STATE]"
    const val NAVIGATION = "[NAV]"
    
    // Error logging
    const val NETWORK_ERROR = "[NETWORK-ERR]"
    const val AUTH_ERROR = "[AUTH-ERR]"
    const val CHAT_ERROR = "[CHAT-ERR]"
}

/**
 * Network logging utilities for detailed request/response logging
 */
object NetworkLogger {
    
    /**
     * Log an HTTP request with details
     * @param method HTTP method (GET, POST, etc.)
     * @param url Full URL being called
     * @param headers Map of request headers
     * @param body Request body (if any)
     * @param tag Logging tag (usually class name)
     */
    fun logRequest(
        method: String,
        url: String,
        headers: Map<String, String> = emptyMap(),
        body: String? = null,
        tag: String = "NetworkCall"
    ) {
        val headerString = if (headers.isNotEmpty()) {
            "\n  Headers: ${headers.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
        } else ""
        
        val bodyString = if (body != null) {
            "\n  Body: $body"
        } else ""
        
        Logger.info("${LogPrefixes.NETWORK_REQUEST} $method $url$headerString$bodyString", tag)
    }
    
    /**
     * Log an HTTP response with details
     * @param method HTTP method that was called
     * @param url URL that was called
     * @param statusCode HTTP status code
     * @param body Response body
     * @param tag Logging tag (usually class name)
     */
    fun logResponse(
        method: String,
        url: String,
        statusCode: Int,
        body: String = "",
        tag: String = "NetworkCall"
    ) {
        val responseBody = if (body.isNotEmpty()) {
            "\n  Response: $body"
        } else ""
        
        Logger.info("${LogPrefixes.NETWORK_RESPONSE} $method $url -> $statusCode$responseBody", tag)
    }
    
    /**
     * Log a network error
     * @param method HTTP method that failed
     * @param url URL that failed
     * @param error Error details
     * @param tag Logging tag (usually class name)
     */
    fun logError(
        method: String,
        url: String,
        error: String,
        tag: String = "NetworkCall"
    ) {
        Logger.error("${LogPrefixes.NETWORK_ERROR} $method $url - $error", tag)
    }
    
    /**
     * Log successful authentication response with session count
     * @param sessionCount Number of sessions returned
     * @param tag Logging tag
     */
    fun logSessionCount(sessionCount: Int, tag: String = "SessionRepo") {
        Logger.info("${LogPrefixes.SESSION_RESPONSE} User has $sessionCount chat sessions", tag)
    }
    
    /**
     * Log session details for debugging
     * @param sessions List of sessions to log
     * @param tag Logging tag
     */
    fun logSessionDetails(sessions: List<Any>, tag: String = "SessionRepo") {
        Logger.info("${LogPrefixes.SESSION_RESPONSE} Session details:", tag)
        sessions.forEachIndexed { index, session ->
            Logger.info("  [$index] $session", tag)
        }
    }
}