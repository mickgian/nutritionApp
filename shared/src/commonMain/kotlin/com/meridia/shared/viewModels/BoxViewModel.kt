package com.meridia.shared.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meridia.shared.auth.AuthModule
import com.meridia.shared.models.BoxDto
import com.meridia.shared.models.OrderDto
import com.meridia.shared.models.PlanDto
import com.meridia.shared.network.BoxRepository
import com.meridia.shared.network.BoxRepositoryImpl
import com.meridia.shared.network.OrderRepository
import com.meridia.shared.network.OrderRepositoryImpl
import com.meridia.shared.utils.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State of the weekly box screen.
 *
 * `Locked` is the persona-`new` state (no plan assigned). `Content` is the
 * orderable/ordered state (plan + weekly menu); a non-null [Content.order]
 * means the box is already ordered (in preparation), otherwise it's orderable
 * (deadline banner + "Ordina", DEV-041).
 */
sealed interface BoxUiState {
    data object Loading : BoxUiState
    data object Locked : BoxUiState
    data class Error(val message: String) : BoxUiState
    data class Content(
        val plan: PlanDto,
        val box: BoxDto,
        val order: OrderDto? = null,
        val selectedWeekday: Int = 0,
    ) : BoxUiState
}

class BoxViewModel(
    private val repo: BoxRepository? = null,
    private val orderRepo: OrderRepository? = null,
) : ViewModel() {

    private val _state = MutableStateFlow<BoxUiState>(BoxUiState.Loading)
    val state: StateFlow<BoxUiState> = _state.asStateFlow()

    private fun repository(): BoxRepository =
        repo ?: BoxRepositoryImpl(authManager = AuthModule.getAuthManager())

    private fun orderRepository(): OrderRepository =
        orderRepo ?: OrderRepositoryImpl(authManager = AuthModule.getAuthManager())

    fun load() {
        _state.value = BoxUiState.Loading
        viewModelScope.launch {
            runCatching {
                val plan = repository().myPlan() ?: return@runCatching null
                val box = repository().box(0)
                val order = orderRepository().myOrders().firstOrNull { it.status != "cancelled" }
                Triple(plan, box, order)
            }
                .onSuccess { result ->
                    _state.value =
                        if (result == null) {
                            BoxUiState.Locked
                        } else {
                            BoxUiState.Content(
                                plan = result.first,
                                box = result.second,
                                order = result.third,
                            )
                        }
                }
                .onFailure {
                    _state.value = BoxUiState.Error(ErrorHandler.handleHttpError(it))
                }
        }
    }

    fun selectWeekday(weekday: Int) {
        val content = _state.value as? BoxUiState.Content ?: return
        _state.value = content.copy(selectedWeekday = weekday)
    }
}
