package com.meridia.shared.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meridia.shared.auth.AuthModule
import com.meridia.shared.models.AppointmentDto
import com.meridia.shared.models.CreditDto
import com.meridia.shared.models.PlanDto
import com.meridia.shared.network.AppointmentRepository
import com.meridia.shared.network.AppointmentRepositoryImpl
import com.meridia.shared.network.BoxRepository
import com.meridia.shared.network.BoxRepositoryImpl
import com.meridia.shared.network.CreditRepository
import com.meridia.shared.network.CreditRepositoryImpl
import com.meridia.shared.network.OrderRepository
import com.meridia.shared.network.OrderRepositoryImpl
import com.meridia.shared.network.PrivacyRepository
import com.meridia.shared.network.PrivacyRepositoryImpl
import com.meridia.shared.utils.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The profilo tab: aggregates the client's own data from /me/* — upcoming
 * appointment, active credits, plan, and box orders — and drives the cancel
 * action (with the 48h credit messaging, DEV-050/051) plus the GDPR
 * export/erasure self-service (DEV-093).
 */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Error(val message: String) : ProfileUiState
    data class Content(
        val upcomingAppointment: AppointmentDto?,
        val credits: List<CreditDto>,
        val plan: PlanDto?,
        val boxCount: Int,
        val subscriptionActive: Boolean,
        val cancelling: Boolean = false,
        val notice: String? = null,
        val privacyBusy: Boolean = false,
        val privacyNotice: String? = null,
        val exportedJson: String? = null,
    ) : ProfileUiState
}

class ProfileViewModel(
    private val appointments: AppointmentRepository? = null,
    private val credits: CreditRepository? = null,
    private val boxes: BoxRepository? = null,
    private val orders: OrderRepository? = null,
    private val privacy: PrivacyRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    private fun appointmentRepo(): AppointmentRepository =
        appointments ?: AppointmentRepositoryImpl(authManager = AuthModule.getAuthManager())

    private fun creditRepo(): CreditRepository =
        credits ?: CreditRepositoryImpl(authManager = AuthModule.getAuthManager())

    private fun boxRepo(): BoxRepository =
        boxes ?: BoxRepositoryImpl(authManager = AuthModule.getAuthManager())

    private fun orderRepo(): OrderRepository =
        orders ?: OrderRepositoryImpl(authManager = AuthModule.getAuthManager())

    private fun privacyRepo(): PrivacyRepository =
        privacy ?: PrivacyRepositoryImpl(authManager = AuthModule.getAuthManager())

    /** Loads the profile. A [notice] (e.g. after a cancellation) is kept on the new state. */
    fun load(notice: String? = null) {
        if (notice == null) _state.value = ProfileUiState.Loading
        viewModelScope.launch {
            runCatching {
                val appts = appointmentRepo().myAppointments()
                val creditList = creditRepo().myCredits()
                val plan = boxRepo().myPlan()
                val orderList = orderRepo().myOrders()
                ProfileUiState.Content(
                    upcomingAppointment = appts.firstOrNull { it.status != "cancelled" },
                    credits = creditList,
                    plan = plan,
                    boxCount = orderList.count { it.status != "cancelled" },
                    subscriptionActive = orderList.any {
                        it.formula == "subscription" && it.status != "cancelled"
                    },
                    notice = notice,
                )
            }
                .onSuccess { _state.value = it }
                .onFailure { _state.value = ProfileUiState.Error(ErrorHandler.handleHttpError(it)) }
        }
    }

    fun cancel(appointmentId: Int) {
        val content = _state.value as? ProfileUiState.Content ?: return
        if (content.cancelling) return
        _state.value = content.copy(cancelling = true, notice = null)
        viewModelScope.launch {
            runCatching { appointmentRepo().cancel(appointmentId) }
                .onSuccess { result ->
                    val credit = result.credit
                    val notice =
                        if (credit != null) {
                            "Appuntamento cancellato. € ${credit.amountCents / 100} di credito " +
                                "accreditati (validi 6 mesi)."
                        } else {
                            "Appuntamento cancellato. Nessun rimborso: preavviso inferiore a 48 ore."
                        }
                    load(notice = notice)
                }
                .onFailure {
                    _state.value = content.copy(
                        cancelling = false,
                        notice = ErrorHandler.handleHttpError(it),
                    )
                }
        }
    }

    /** GDPR export: fetch the caller's full data document (kept in state for sharing). */
    fun exportData() {
        val content = _state.value as? ProfileUiState.Content ?: return
        if (content.privacyBusy) return
        _state.value = content.copy(privacyBusy = true, privacyNotice = null)
        viewModelScope.launch {
            runCatching { privacyRepo().exportData() }
                .onSuccess { data ->
                    updateContent {
                        it.copy(
                            privacyBusy = false,
                            exportedJson = data,
                            privacyNotice = "Esportazione completata: i tuoi dati sono pronti.",
                        )
                    }
                }
                .onFailure { e ->
                    updateContent {
                        it.copy(privacyBusy = false, privacyNotice = ErrorHandler.handleHttpError(e))
                    }
                }
        }
    }

    /** GDPR erasure: delete the account, then hand control back to [onDeleted] (logout). */
    fun deleteAccount(onDeleted: () -> Unit) {
        val content = _state.value as? ProfileUiState.Content ?: return
        if (content.privacyBusy) return
        _state.value = content.copy(privacyBusy = true, privacyNotice = null)
        viewModelScope.launch {
            runCatching { privacyRepo().deleteAccount() }
                .onSuccess { onDeleted() }
                .onFailure { e ->
                    updateContent {
                        it.copy(privacyBusy = false, privacyNotice = ErrorHandler.handleHttpError(e))
                    }
                }
        }
    }

    private inline fun updateContent(transform: (ProfileUiState.Content) -> ProfileUiState.Content) {
        val content = _state.value as? ProfileUiState.Content ?: return
        _state.value = transform(content)
    }
}
