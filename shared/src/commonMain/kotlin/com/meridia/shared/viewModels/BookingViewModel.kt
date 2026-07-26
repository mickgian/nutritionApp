package com.meridia.shared.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meridia.shared.auth.AuthModule
import com.meridia.shared.models.AppointmentDto
import com.meridia.shared.models.SlotDto
import com.meridia.shared.models.VisitTypeUi
import com.meridia.shared.network.AppointmentRepository
import com.meridia.shared.network.AppointmentRepositoryImpl
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

/** The three steps of the booking wizard (data → riepilogo → pagamento). */
enum class BookingStep { Slot, Riepilogo, Pagamento }

sealed interface BookingUiState {
    data object Loading : BookingUiState
    data object Empty : BookingUiState
    data class Error(val message: String) : BookingUiState
    data class Content(
        val slots: List<SlotDto>,
        val step: BookingStep = BookingStep.Slot,
        val selectedSlot: SlotDto? = null,
        val visitType: VisitTypeUi = VisitTypeUi.Prima,
        val submitting: Boolean = false,
        val submitError: String? = null,
    ) : BookingUiState

    data class Confirmed(val appointment: AppointmentDto) : BookingUiState
}

class BookingViewModel(private val repo: AppointmentRepository? = null) : ViewModel() {

    private val _state = MutableStateFlow<BookingUiState>(BookingUiState.Loading)
    val state: StateFlow<BookingUiState> = _state.asStateFlow()

    private fun repository(): AppointmentRepository =
        repo ?: AppointmentRepositoryImpl(authManager = AuthModule.getAuthManager())

    fun load() {
        _state.value = BookingUiState.Loading
        viewModelScope.launch {
            runCatching { repository().availability(fromDate(), toDate()) }
                .onSuccess { slots ->
                    _state.value =
                        if (slots.isEmpty()) BookingUiState.Empty
                        else BookingUiState.Content(slots = slots)
                }
                .onFailure {
                    _state.value = BookingUiState.Error(ErrorHandler.handleHttpError(it))
                }
        }
    }

    fun selectSlot(slot: SlotDto) = updateContent { it.copy(selectedSlot = slot) }

    fun setVisitType(visitType: VisitTypeUi) = updateContent { it.copy(visitType = visitType) }

    fun next() = updateContent { content ->
        when (content.step) {
            BookingStep.Slot ->
                if (content.selectedSlot != null) content.copy(step = BookingStep.Riepilogo)
                else content
            BookingStep.Riepilogo -> content.copy(step = BookingStep.Pagamento)
            BookingStep.Pagamento -> content
        }
    }

    fun back() = updateContent { content ->
        when (content.step) {
            BookingStep.Slot -> content
            BookingStep.Riepilogo -> content.copy(step = BookingStep.Slot)
            BookingStep.Pagamento -> content.copy(step = BookingStep.Riepilogo)
        }
    }

    fun confirm() {
        val content = _state.value as? BookingUiState.Content ?: return
        val slot = content.selectedSlot ?: return
        _state.value = content.copy(submitting = true, submitError = null)
        viewModelScope.launch {
            runCatching { repository().book(slot.id, content.visitType.wire) }
                .onSuccess { _state.value = BookingUiState.Confirmed(it) }
                .onFailure {
                    _state.value = content.copy(
                        submitting = false,
                        submitError = ErrorHandler.handleHttpError(it),
                    )
                }
        }
    }

    private inline fun updateContent(transform: (BookingUiState.Content) -> BookingUiState.Content) {
        val content = _state.value as? BookingUiState.Content ?: return
        _state.value = transform(content)
    }

    private fun fromDate(): String = Clock.System.todayIn(TimeZone.UTC).toString()

    private fun toDate(): String =
        Clock.System.todayIn(TimeZone.UTC).plus(30, DateTimeUnit.DAY).toString()
}
