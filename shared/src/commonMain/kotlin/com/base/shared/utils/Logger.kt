package com.base.shared.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object Logger {
    private const val TAG = "BaseApp"
    
    enum class Level {
        DEBUG, INFO, WARN, ERROR
    }
    
    fun debug(message: String, tag: String = TAG) {
        log(Level.DEBUG, tag, message)
    }
    
    fun info(message: String, tag: String = TAG) {
        log(Level.INFO, tag, message)
    }
    
    fun warn(message: String, tag: String = TAG) {
        log(Level.WARN, tag, message)
    }
    
    fun error(message: String, tag: String = TAG, throwable: Throwable? = null) {
        val fullMessage = throwable?.let { "$message\n${it.stackTraceToString()}" } ?: message
        log(Level.ERROR, tag, fullMessage)
    }
    
    // Auth-specific logging methods
    fun authDebug(operation: String, details: String = "") {
        val message = "AUTH_$operation${if (details.isNotEmpty()) " | $details" else ""}"
        debug(message, "BaseApp_Auth")
    }
    
    fun authInfo(operation: String, details: String = "") {
        val message = "AUTH_$operation${if (details.isNotEmpty()) " | $details" else ""}"
        info(message, "BaseApp_Auth")
    }
    
    fun authWarn(operation: String, details: String = "") {
        val message = "AUTH_$operation${if (details.isNotEmpty()) " | $details" else ""}"
        warn(message, "BaseApp_Auth")
    }
    
    fun authError(operation: String, error: String, throwable: Throwable? = null) {
        val message = "AUTH_$operation | ERROR: $error"
        error(message, "BaseApp_Auth", throwable)
    }
    
    // Token logging helper - logs first 8 characters only for security
    fun logToken(tokenType: String, token: String, operation: String) {
        val tokenPreview = if (token.length >= 8) {
            "${token.take(8)}..."
        } else {
            "${token}..."
        }
        authInfo(operation, "$tokenType token: $tokenPreview")
    }
    
    // Timestamp helper for consistent formatting
    private fun getTimestamp(): String {
        val now = Clock.System.now()
        val localDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${localDateTime.hour.toString().padStart(2, '0')}:" +
                "${localDateTime.minute.toString().padStart(2, '0')}:" +
                "${localDateTime.second.toString().padStart(2, '0')}"
    }
    
    private fun formatMessage(level: Level, tag: String, message: String): String {
        return "[${getTimestamp()}] [${level.name}] [$tag] $message"
    }
    
    // Platform-specific implementation will be provided by expect/actual
    private fun log(level: Level, tag: String, message: String) {
        platformLog(level, tag, formatMessage(level, tag, message))
    }
}

// Platform-specific logging implementation
expect fun platformLog(level: Logger.Level, tag: String, message: String)