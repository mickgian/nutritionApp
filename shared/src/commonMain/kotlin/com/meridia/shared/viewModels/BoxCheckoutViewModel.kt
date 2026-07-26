package com.meridia.shared.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meridia.shared.auth.AuthModule
import com.meridia.shared.models.OrderDto
import com.meridia.shared.network.OrderRepository
import com.meridia.shared.network.OrderRepositoryImpl
import com.meridia.shared.utils.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The box formula. Prices are shown for display only; the backend recomputes
 * the authoritative amount (Rule 7). Subscription is the default, as in the demo.
 */
enum class OrderFormula(val wire: String, val priceEur: Int) {
    Subscription("subscription", 79),
    Single("single", 89),
}

/** Pickup window on the Monday collection day. */
enum class PickupSlot(val wire: String, val window: String) {
    Mattina("mattina", "8:30-12:30"),
    Pomeriggio("pomeriggio", "15:00-19:00"),
}

/**
 * State of the box checkout sheet: the current selections plus submit/success.
 * A non-null [placedOrder] is the success state (box now ordered).
 */
data class BoxCheckoutState(
    val formula: OrderFormula = OrderFormula.Subscription,
    val pickup: PickupSlot = PickupSlot.Mattina,
    val submitting: Boolean = false,
    val error: String? = null,
    val placedOrder: OrderDto? = null,
)

class BoxCheckoutViewModel(private val repo: OrderRepository? = null) : ViewModel() {

    private val _state = MutableStateFlow(BoxCheckoutState())
    val state: StateFlow<BoxCheckoutState> = _state.asStateFlow()

    private fun repository(): OrderRepository =
        repo ?: OrderRepositoryImpl(authManager = AuthModule.getAuthManager())

    fun selectFormula(formula: OrderFormula) {
        _state.value = _state.value.copy(formula = formula)
    }

    fun selectPickup(pickup: PickupSlot) {
        _state.value = _state.value.copy(pickup = pickup)
    }

    fun place() {
        val current = _state.value
        if (current.submitting || current.placedOrder != null) return
        _state.value = current.copy(submitting = true, error = null)
        viewModelScope.launch {
            runCatching { repository().createOrder(current.formula.wire, current.pickup.wire) }
                .onSuccess { _state.value = _state.value.copy(submitting = false, placedOrder = it) }
                .onFailure {
                    _state.value = _state.value.copy(
                        submitting = false,
                        error = ErrorHandler.handleHttpError(it),
                    )
                }
        }
    }
}
