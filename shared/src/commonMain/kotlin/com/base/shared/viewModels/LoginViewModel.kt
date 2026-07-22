package com.base.shared.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.base.shared.auth.AuthModule
import com.base.shared.models.TokenResponse
import com.base.shared.network.auth.AuthRepository
import com.base.shared.utils.ErrorHandler
import com.base.shared.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repo: AuthRepository? = null
) : ViewModel() {
    
    private fun getRepository(): AuthRepository {
        return repo ?: if (AuthModule.isInitialized()) {
            AuthModule.getAuthRepository()
        } else {
            throw IllegalStateException("AuthModule not initialized")
        }
    }

    /** UI-facing state */
    sealed interface LoginState {
        object Idle : LoginState
        object Loading : LoginState
        data class Success(val token: TokenResponse) : LoginState
        data class Error(val message: String) : LoginState
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    /** Triggered when the user presses "Log in" */
    fun login(username: String, password: String) {
        Logger.authInfo("VIEWMODEL_LOGIN_START", "LoginViewModel.login() called for username: $username")
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            runCatching { getRepository().loginAndSaveToken(username, password) }
                .onSuccess { 
                    Logger.authInfo("VIEWMODEL_LOGIN_SUCCESS", "LoginViewModel login successful")
                    _loginState.value = LoginState.Success(it) 
                }
                .onFailure { 
                    val userMessage = ErrorHandler.handleHttpError(it)
                    Logger.authError("VIEWMODEL_LOGIN_FAILED", "LoginViewModel login failed: ${it.message}")
                    _loginState.value = LoginState.Error(userMessage) 
                }
        }
    }
}