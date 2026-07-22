package com.base.shared.viewModels

import com.base.shared.auth.AuthModule
import com.base.shared.network.auth.AuthRepository
import com.base.shared.utils.ErrorHandler
import com.base.shared.utils.Logger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RegistrationViewModel(
    private val repository: AuthRepository? = null
) {
    
    private fun getRepository(): AuthRepository {
        return repository ?: if (AuthModule.isInitialized()) {
            AuthModule.getAuthRepository()
        } else {
            throw IllegalStateException("AuthModule not initialized")
        }
    }
    sealed interface State {
        object Idle    : State
        object Loading : State
        data class Error(val message: String) : State
        object Success : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    // scope for launching coroutines on the Main thread, with error handler
    private val viewModelScope = CoroutineScope(
        Dispatchers.Main + SupervisorJob() +
                CoroutineExceptionHandler { _, e ->
                    val userMessage = ErrorHandler.handleHttpError(e)
                    Logger.authError("VIEWMODEL_REG_EXCEPTION", "RegistrationViewModel exception: ${e.message}", e)
                    _state.value = State.Error(userMessage)
                }
    )

    /**
     * Kick off the registration network call.
     *
     * @param email the user's email
     * @param password the chosen password
     */
    fun register(email: String, password: String) {
        Logger.authInfo("VIEWMODEL_REG_START", "RegistrationViewModel.register() called for email: $email")
        viewModelScope.launch {
            _state.value = State.Loading
            try {
                // call our Ktor-based repository
                val response = getRepository().register(email, password)
                Logger.authInfo("VIEWMODEL_REG_SUCCESS", "RegistrationViewModel registration successful for user ID: ${response.id}")
                _state.value = State.Success
            } catch (e: Exception) {
                val userMessage = ErrorHandler.handleHttpError(e)
                Logger.authError("VIEWMODEL_REG_FAILED", "RegistrationViewModel registration failed: ${e.message}", e)
                _state.value = State.Error(userMessage)
            }
        }
    }
}