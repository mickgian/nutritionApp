package com.meridia.shared.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meridia.shared.auth.AuthModule
import com.meridia.shared.models.MealDto
import com.meridia.shared.network.MealRepository
import com.meridia.shared.network.MealRepositoryImpl
import com.meridia.shared.utils.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MealDetailUiState {
    data object Loading : MealDetailUiState
    data class Error(val message: String) : MealDetailUiState
    data class Content(val meal: MealDto) : MealDetailUiState
}

class MealDetailViewModel(private val repo: MealRepository? = null) : ViewModel() {

    private val _state = MutableStateFlow<MealDetailUiState>(MealDetailUiState.Loading)
    val state: StateFlow<MealDetailUiState> = _state.asStateFlow()

    private fun repository(): MealRepository =
        repo ?: MealRepositoryImpl(authManager = AuthModule.getAuthManager())

    fun load(mealId: Int) {
        _state.value = MealDetailUiState.Loading
        viewModelScope.launch {
            runCatching { repository().meal(mealId) }
                .onSuccess { _state.value = MealDetailUiState.Content(it) }
                .onFailure {
                    _state.value = MealDetailUiState.Error(ErrorHandler.handleHttpError(it))
                }
        }
    }
}
