package com.meridia.shared.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meridia.shared.auth.AuthModule
import com.meridia.shared.models.AdminClientDto
import com.meridia.shared.models.AdminOrderDto
import com.meridia.shared.network.AdminRepository
import com.meridia.shared.network.AdminRepositoryImpl
import com.meridia.shared.utils.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The studio dashboard (DEV-080): the client directory (with plan assignment)
 * and the box-order overview, plus the promotion broadcast. Every action hits an
 * admin-guarded endpoint; the server is authoritative and a non-admin gets 403.
 */
sealed interface AdminUiState {
    data object Loading : AdminUiState

    data class Error(val message: String) : AdminUiState

    data class Content(
        val clients: List<AdminClientDto>,
        val orders: List<AdminOrderDto>,
        val assigningClientId: Int? = null,
        val promoSubmitting: Boolean = false,
        val promoNotice: String? = null,
        val actionError: String? = null,
    ) : AdminUiState
}

class AdminViewModel(private val repo: AdminRepository? = null) : ViewModel() {

    private val _state = MutableStateFlow<AdminUiState>(AdminUiState.Loading)
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    private fun repository(): AdminRepository =
        repo ?: AdminRepositoryImpl(authManager = AuthModule.getAuthManager())

    fun load() {
        _state.value = AdminUiState.Loading
        viewModelScope.launch {
            runCatching { repository().clients() to repository().orders() }
                .onSuccess { (clients, orders) ->
                    _state.value = AdminUiState.Content(clients = clients, orders = orders)
                }
                .onFailure {
                    _state.value = AdminUiState.Error(ErrorHandler.handleHttpError(it))
                }
        }
    }

    fun assignPlan(clientId: Int, name: String, dailyKcal: Int, weeks: Int) {
        val content = _state.value as? AdminUiState.Content ?: return
        if (content.assigningClientId != null) return
        _state.value = content.copy(assigningClientId = clientId, actionError = null)
        viewModelScope.launch {
            runCatching {
                repository().assignPlan(clientId, name, dailyKcal, weeks)
                repository().clients()
            }
                .onSuccess { clients -> updateContent { it.copy(clients = clients, assigningClientId = null) } }
                .onFailure { e ->
                    updateContent {
                        it.copy(assigningClientId = null, actionError = ErrorHandler.handleHttpError(e))
                    }
                }
        }
    }

    fun sendPromotion(title: String, body: String) {
        val content = _state.value as? AdminUiState.Content ?: return
        if (content.promoSubmitting) return
        if (title.isBlank() || body.isBlank()) {
            updateContent { it.copy(actionError = "Inserisci titolo e messaggio della promozione.") }
            return
        }
        _state.value = content.copy(promoSubmitting = true, promoNotice = null, actionError = null)
        viewModelScope.launch {
            runCatching { repository().sendPromotion(title.trim(), body.trim(), "📣") }
                .onSuccess { result ->
                    updateContent {
                        it.copy(
                            promoSubmitting = false,
                            promoNotice = "Promozione inviata a ${result.recipients} " +
                                clientiWord(result.recipients),
                        )
                    }
                }
                .onFailure { e ->
                    updateContent {
                        it.copy(promoSubmitting = false, actionError = ErrorHandler.handleHttpError(e))
                    }
                }
        }
    }

    private inline fun updateContent(transform: (AdminUiState.Content) -> AdminUiState.Content) {
        val content = _state.value as? AdminUiState.Content ?: return
        _state.value = transform(content)
    }

    private fun clientiWord(count: Int): String = if (count == 1) "cliente" else "clienti"
}
