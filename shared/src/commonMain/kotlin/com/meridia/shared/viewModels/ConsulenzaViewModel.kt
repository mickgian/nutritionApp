package com.meridia.shared.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meridia.shared.auth.AuthModule
import com.meridia.shared.models.ProfessionalDto
import com.meridia.shared.network.ProfessionalRepository
import com.meridia.shared.network.ProfessionalRepositoryImpl
import com.meridia.shared.network.auth.AuthRepository
import com.meridia.shared.network.auth.AuthRepositoryImpl
import com.meridia.shared.utils.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ConsulenzaUiState {
    data object Loading : ConsulenzaUiState
    data class Error(val message: String) : ConsulenzaUiState
    data class Content(
        val professional: ProfessionalDto,
        // Drives the studio-panel entry point; the server enforces the role too.
        val isAdmin: Boolean = false,
    ) : ConsulenzaUiState
}

private const val ADMIN_ROLE = "admin"

class ConsulenzaViewModel(
    private val repo: ProfessionalRepository? = null,
    private val authRepo: AuthRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow<ConsulenzaUiState>(ConsulenzaUiState.Loading)
    val state: StateFlow<ConsulenzaUiState> = _state.asStateFlow()

    private fun repository(): ProfessionalRepository =
        repo ?: ProfessionalRepositoryImpl(authManager = AuthModule.getAuthManager())

    private fun authRepository(): AuthRepository =
        authRepo ?: AuthRepositoryImpl(authManager = AuthModule.getAuthManager())

    fun load() {
        _state.value = ConsulenzaUiState.Loading
        viewModelScope.launch {
            runCatching { repository().profile() }
                .onSuccess { professional ->
                    // Role is best-effort: a lookup failure just hides the studio entry.
                    val isAdmin = runCatching { authRepository().me().role == ADMIN_ROLE }
                        .getOrDefault(false)
                    _state.value = ConsulenzaUiState.Content(professional, isAdmin)
                }
                .onFailure {
                    _state.value = ConsulenzaUiState.Error(ErrorHandler.handleHttpError(it))
                }
        }
    }
}
