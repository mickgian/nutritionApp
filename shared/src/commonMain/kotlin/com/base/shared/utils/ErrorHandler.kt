package com.base.shared.utils

import com.base.shared.models.ErrorResponse
import com.base.shared.models.ValidationErrorResponse
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

object ErrorHandler {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Converts HTTP exceptions to user-friendly error messages
     */
    fun handleHttpError(exception: Throwable): String {
        return when (exception) {
            is ResponseException -> {
                when (exception.response.status.value) {
                    400 -> handleBadRequest(exception)
                    401 -> handleUnauthorized(exception)
                    422 -> handleValidationError(exception)
                    429 -> "Too many requests. Please try again later."
                    500, 502, 503, 504 -> "Server error. Please try again later."
                    else -> parseErrorMessage(exception) ?: "An unexpected error occurred"
                }
            }
            is ClientRequestException -> {
                when (exception.response.status.value) {
                    400 -> handleBadRequest(exception)
                    401 -> "Invalid credentials. Please check your email and password."
                    404 -> "Service not found. Please try again later."
                    422 -> handleValidationError(exception)
                    else -> "Request failed. Please try again."
                }
            }
            is ServerResponseException -> {
                "Server error. Please try again later."
            }
            else -> exception.message?.let { parseGenericError(it) } ?: "An unexpected error occurred"
        }
    }

    private fun handleBadRequest(exception: ResponseException): String {
        val errorMessage = parseErrorMessage(exception)
        return when {
            errorMessage?.contains("already registered", ignoreCase = true) == true -> 
                "This email is already registered. Please use a different email or try logging in."
            errorMessage?.contains("grant type", ignoreCase = true) == true ->
                "Authentication error. Please try again."
            else -> errorMessage ?: "Invalid request. Please check your input."
        }
    }

    private fun handleUnauthorized(exception: ResponseException): String {
        val errorMessage = parseErrorMessage(exception)
        return when {
            errorMessage?.contains("email", ignoreCase = true) == true ||
            errorMessage?.contains("password", ignoreCase = true) == true ||
            errorMessage?.contains("credentials", ignoreCase = true) == true ->
                "Invalid email or password. Please check your credentials and try again."
            else -> "Authentication failed. Please try again."
        }
    }

    private fun handleValidationError(exception: ResponseException): String {
        return try {
            // Try to parse from exception message first
            val exceptionMessage = exception.message ?: ""
            if (exceptionMessage.contains("{") && exceptionMessage.contains("}")) {
                val validationError = json.decodeFromString<ValidationErrorResponse>(exceptionMessage)
                
                if (validationError.errors?.isNotEmpty() == true) {
                    // Convert validation errors to user-friendly messages
                    validationError.errors.joinToString("\n") { error ->
                        formatValidationError(error.field, error.message)
                    }
                } else {
                    formatGeneralValidationMessage(validationError.detail)
                }
            } else {
                // Fallback: analyze the message directly
                formatGeneralValidationMessage(exceptionMessage)
            }
        } catch (_: SerializationException) {
            // Fallback: parse common validation patterns from message
            val message = exception.message ?: ""
            formatGeneralValidationMessage(message)
        } catch (_: Exception) {
            "Please check your input and try again."
        }
    }

    private fun formatValidationError(field: String, message: String): String {
        return when (field.lowercase()) {
            "email" -> when {
                message.contains("required", ignoreCase = true) -> "Email is required."
                message.contains("format", ignoreCase = true) || message.contains("valid", ignoreCase = true) -> 
                    "Please enter a valid email address."
                else -> "Email: $message"
            }
            "password" -> when {
                message.contains("required", ignoreCase = true) -> "Password is required."
                message.contains("8 characters", ignoreCase = true) || message.contains("8 items", ignoreCase = true) -> 
                    "Password must be at least 8 characters long."
                message.contains("uppercase", ignoreCase = true) -> 
                    "Password must contain at least one uppercase letter."
                message.contains("lowercase", ignoreCase = true) -> 
                    "Password must contain at least one lowercase letter."
                message.contains("number", ignoreCase = true) || message.contains("digit", ignoreCase = true) -> 
                    "Password must contain at least one number."
                message.contains("special", ignoreCase = true) -> 
                    "Password must contain at least one special character (!@#$%^&*(),.?\":{}|<>)."
                else -> "Password: $message"
            }
            else -> "$field: $message"
        }
    }

    private fun formatGeneralValidationMessage(detail: String): String {
        return when {
            detail.contains("8 characters", ignoreCase = true) || detail.contains("8 items", ignoreCase = true) -> 
                "Password must be at least 8 characters long."
            detail.contains("uppercase", ignoreCase = true) -> 
                "Password must contain at least one uppercase letter."
            detail.contains("lowercase", ignoreCase = true) -> 
                "Password must contain at least one lowercase letter."
            detail.contains("number", ignoreCase = true) || detail.contains("digit", ignoreCase = true) -> 
                "Password must contain at least one number."
            detail.contains("special", ignoreCase = true) -> 
                "Password must contain at least one special character (!@#$%^&*(),.?\":{}|<>)."
            detail.contains("email", ignoreCase = true) && detail.contains("format", ignoreCase = true) -> 
                "Please enter a valid email address."
            detail.contains("already registered", ignoreCase = true) -> 
                "This email is already registered. Please use a different email or try logging in."
            else -> detail
        }
    }

    private fun parseErrorMessage(exception: ResponseException): String? {
        return try {
            val exceptionMessage = exception.message ?: ""
            if (exceptionMessage.contains("{") && exceptionMessage.contains("}")) {
                val errorResponse = json.decodeFromString<ErrorResponse>(exceptionMessage)
                errorResponse.detail
            } else {
                exceptionMessage.takeIf { it.isNotBlank() }
            }
        } catch (_: SerializationException) {
            exception.message
        } catch (_: Exception) {
            null
        }
    }

    private fun parseGenericError(message: String): String {
        return when {
            message.contains("required for type", ignoreCase = true) -> 
                "Authentication failed. Please check your credentials."
            message.contains("connect", ignoreCase = true) -> 
                "Unable to connect to server. Please check your internet connection."
            message.contains("timeout", ignoreCase = true) -> 
                "Request timed out. Please try again."
            else -> message
        }
    }
}