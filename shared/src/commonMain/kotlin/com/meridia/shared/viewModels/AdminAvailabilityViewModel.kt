package com.meridia.shared.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meridia.shared.auth.AuthModule
import com.meridia.shared.models.AdminSlotDto
import com.meridia.shared.network.AdminRepository
import com.meridia.shared.network.AdminRepositoryImpl
import com.meridia.shared.utils.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

/**
 * The studio availability panel (DEV-080, consumes DEV-020): the upcoming slots
 * the studio can open or close for booking. Toggling a slot PUTs its new state;
 * a slot already held by an appointment can't be closed (server returns 409).
 */
sealed interface AdminAvailabilityUiState {
    data object Loading : AdminAvailabilityUiState

    data class Error(val message: String) : AdminAvailabilityUiState

    data class Content(
        val slots: List<AdminSlotDto>,
        val togglingSlotId: Int? = null,
        val error: String? = null,
    ) : AdminAvailabilityUiState
}

private const val WINDOW_DAYS = 21

class AdminAvailabilityViewModel(private val repo: AdminRepository? = null) : ViewModel() {

    private val _state = MutableStateFlow<AdminAvailabilityUiState>(AdminAvailabilityUiState.Loading)
    val state: StateFlow<AdminAvailabilityUiState> = _state.asStateFlow()

    private fun repository(): AdminRepository =
        repo ?: AdminRepositoryImpl(authManager = AuthModule.getAuthManager())

    fun load() {
        _state.value = AdminAvailabilityUiState.Loading
        viewModelScope.launch {
            runCatching { repository().availability(fromDate(), toDate()) }
                .onSuccess { _state.value = AdminAvailabilityUiState.Content(slots = it) }
                .onFailure {
                    _state.value = AdminAvailabilityUiState.Error(ErrorHandler.handleHttpError(it))
                }
        }
    }

    fun toggle(slot: AdminSlotDto) {
        val content = _state.value as? AdminAvailabilityUiState.Content ?: return
        if (content.togglingSlotId != null) return
        _state.value = content.copy(togglingSlotId = slot.id, error = null)
        viewModelScope.launch {
            runCatching {
                repository().setAvailability(slot.startsAt, slot.durationMin, !slot.isOpen)
            }
                .onSuccess { updated -> replaceSlot(updated) }
                .onFailure { e -> failToggle(ErrorHandler.handleHttpError(e)) }
        }
    }

    private fun replaceSlot(updated: AdminSlotDto) {
        val content = _state.value as? AdminAvailabilityUiState.Content ?: return
        _state.value = content.copy(
            slots = content.slots.map { if (it.id == updated.id) updated else it },
            togglingSlotId = null,
        )
    }

    private fun failToggle(message: String) {
        val content = _state.value as? AdminAvailabilityUiState.Content ?: return
        _state.value = content.copy(togglingSlotId = null, error = message)
    }

    private fun fromDate(): String = Clock.System.todayIn(TimeZone.UTC).toString()

    private fun toDate(): String =
        Clock.System.todayIn(TimeZone.UTC).plus(WINDOW_DAYS, DateTimeUnit.DAY).toString()
}
