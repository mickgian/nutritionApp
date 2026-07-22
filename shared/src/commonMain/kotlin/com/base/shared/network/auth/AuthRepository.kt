package com.base.shared.network.auth


import com.base.shared.ApiConfig
import com.base.shared.auth.AuthManager
import com.base.shared.models.AuthResponse
import com.base.shared.models.ErrorResponse
import com.base.shared.models.RegisterRequest
import com.base.shared.models.TokenResponse
import com.base.shared.models.TokenWithExpiry
import com.base.shared.models.ValidationErrorResponse
import com.base.shared.network.HttpClientProvider
import com.base.shared.utils.Logger
import com.base.shared.utils.ErrorHandler
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json


interface AuthRepository {
    suspend fun register(email: String, password: String): AuthResponse
    suspend fun login(username: String, password: String): TokenResponse
    suspend fun loginAndSaveToken(username: String, password: String): TokenResponse
}

class AuthRepositoryImpl(
    private val client: io.ktor.client.HttpClient = HttpClientProvider.client,
    private val authManager: AuthManager? = null
) : AuthRepository {

    private val base = ApiConfig.BASE_URL

    override suspend fun register(
        email: String,
        password: String
    ): AuthResponse {
        Logger.authInfo("REGISTER_START", "Starting registration for email: $email")
        try {
            val httpResponse = client.post("$base/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(RegisterRequest(email, password))
            }
            
            if (httpResponse.status.isSuccess()) {
                val response = httpResponse.body<AuthResponse>()
                Logger.authInfo("REGISTER_SUCCESS", "Registration successful for user ID: ${response.id}")
                Logger.logToken("ACCESS", response.accessToken, "REGISTER_TOKEN_RECEIVED")
                Logger.authInfo("REGISTER_TOKEN_TYPE", "Token type: ${response.tokenType}")
                return response
            } else {
                // Handle error response by reading body and creating proper error message
                val errorBody = httpResponse.bodyAsText()
                Logger.authError("REGISTER_HTTP_ERROR", "Registration failed with status ${httpResponse.status.value}: $errorBody")
                
                val friendlyMessage = when (httpResponse.status.value) {
                    422 -> parseValidationError(errorBody)
                    400 -> parseBadRequestError(errorBody)
                    else -> "Registration failed. Please try again."
                }
                
                throw Exception(friendlyMessage)
            }
        } catch (e: Exception) {
            if (e.message?.startsWith("Password must") == true || 
                e.message?.startsWith("Email") == true ||
                e.message?.contains("already registered") == true) {
                // This is already a friendly message we created above
                Logger.authError("REGISTER_FAILED", "Registration failed for email: $email. Error: ${e.message}", e)
                throw e
            } else {
                // Other exceptions (network, serialization, etc.) - let ErrorHandler process them
                val friendlyMessage = ErrorHandler.handleHttpError(e)
                Logger.authError("REGISTER_FAILED", "Registration failed for email: $email. Error: $friendlyMessage", e)
                throw Exception(friendlyMessage, e)
            }
        }
    }

    private fun parseValidationError(errorBody: String): String {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val validationError = json.decodeFromString<ValidationErrorResponse>(errorBody)
            
            if (validationError.errors?.isNotEmpty() == true) {
                // Group errors by field for better formatting
                val passwordErrors = validationError.errors.filter { it.field.lowercase() == "password" }
                val emailErrors = validationError.errors.filter { it.field.lowercase() == "email" }
                val otherErrors = validationError.errors.filter { 
                    it.field.lowercase() !in listOf("password", "email") 
                }
                
                val messages = mutableListOf<String>()
                
                // Handle password errors with comprehensive requirements
                if (passwordErrors.isNotEmpty()) {
                    val missingRequirements = mutableListOf<String>()
                    
                    passwordErrors.forEach { error ->
                        when {
                            error.message.contains("8 characters", ignoreCase = true) -> 
                                missingRequirements.add("at least 8 characters long")
                            error.message.contains("uppercase", ignoreCase = true) -> 
                                missingRequirements.add("at least one uppercase letter")
                            error.message.contains("lowercase", ignoreCase = true) -> 
                                missingRequirements.add("at least one lowercase letter")
                            error.message.contains("number", ignoreCase = true) -> 
                                missingRequirements.add("at least one number")
                            error.message.contains("special", ignoreCase = true) -> 
                                missingRequirements.add("at least one special character")
                        }
                    }
                    
                    if (missingRequirements.isNotEmpty()) {
                        val requirementText = when (missingRequirements.size) {
                            1 -> "Password must be ${missingRequirements[0]}."
                            2 -> "Password must be ${missingRequirements[0]} and ${missingRequirements[1]}."
                            else -> {
                                val lastRequirement = missingRequirements.last()
                                val otherRequirements = missingRequirements.dropLast(1).joinToString(", ")
                                "Password must be $otherRequirements, and $lastRequirement."
                            }
                        }
                        messages.add(requirementText)
                    }
                }
                
                // Handle email errors
                emailErrors.forEach { error ->
                    when {
                        error.message.contains("format", ignoreCase = true) || 
                        error.message.contains("valid", ignoreCase = true) -> 
                            messages.add("Please enter a valid email address.")
                        else -> messages.add("Email: ${error.message}")
                    }
                }
                
                // Handle other field errors
                otherErrors.forEach { error ->
                    messages.add("${error.field}: ${error.message}")
                }
                
                messages.joinToString("\n")
            } else {
                validationError.detail
            }
        } catch (_: Exception) {
            "Please check your input and try again."
        }
    }

    private fun parseBadRequestError(errorBody: String): String {
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val errorResponse = json.decodeFromString<ErrorResponse>(errorBody)
            when {
                errorResponse.detail.contains("already registered", ignoreCase = true) -> 
                    "This email is already registered. Please use a different email or try logging in."
                else -> errorResponse.detail
            }
        } catch (_: Exception) {
            "Registration failed. Please check your input and try again."
        }
    }

    override suspend fun login(
        username: String,
        password: String
    ): TokenResponse {
        Logger.authInfo("LOGIN_START", "Starting login for username: $username")
        Logger.authInfo("LOGIN_URL", "Login URL: ${base}/auth/login")
        try {
            val response = client.post("${base}/auth/login") {
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("username", username)
                            append("password", password)
                            append("grant_type", "")
                        }
                    )
                )
                contentType(ContentType.Application.FormUrlEncoded)
                accept(ContentType.Application.Json)
            }.body<TokenResponse>()
            
            Logger.authInfo("LOGIN_SUCCESS", "Login successful for username: $username")
            Logger.logToken("ACCESS", response.accessToken, "LOGIN_TOKEN_RECEIVED")
            Logger.authInfo("LOGIN_TOKEN_TYPE", "Token type: ${response.tokenType}")
            Logger.authInfo("LOGIN_TOKEN_EXPIRY", "Token expires at: ${response.expiresAt}")
            return response
        } catch (e: Exception) {
            val friendlyMessage = ErrorHandler.handleHttpError(e)
            Logger.authError("LOGIN_FAILED", "Login failed for username: $username. URL: ${base}/auth/login. Error: $friendlyMessage", e)
            throw Exception(friendlyMessage, e)
        }
    }

    override suspend fun loginAndSaveToken(
        username: String,
        password: String
    ): TokenResponse {
        Logger.authInfo("LOGIN_AND_SAVE_START", "Starting login and save for username: $username")
        val tokenResponse = login(username, password)
        
        authManager?.let { manager ->
            Logger.authInfo("LOGIN_AND_SAVE_CONVERTING", "Converting token response to TokenWithExpiry")
            val tokenWithExpiry = TokenWithExpiry(
                accessToken = tokenResponse.accessToken,
                expiresAt = tokenResponse.expiresAt
            )
            Logger.authInfo("LOGIN_AND_SAVE_DELEGATING", "Delegating token save to AuthManager")
            manager.saveToken(tokenWithExpiry)
        } ?: run {
            Logger.authWarn("LOGIN_AND_SAVE_NO_MANAGER", "No AuthManager provided, token not saved locally")
        }
        
        Logger.authInfo("LOGIN_AND_SAVE_COMPLETE", "Login and save operation completed")
        return tokenResponse
    }
}