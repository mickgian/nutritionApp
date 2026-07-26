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
import com.meridia.shared.utils.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The profilo tab: aggregates the client's own data from /me/* — upcoming
 * appointment, active credits, plan, and box orders — and drives the cancel
 * action (with the 48h credit messaging, DEV-050/051).
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
    ) : ProfileUiState
}

class ProfileViewModel(
    private val appointments: AppointmentRepository? = null,
    private val credits: CreditRepository? = null,
    private val boxes: BoxRepository? = null,
    private val orders: OrderRepository? = null,
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
}
