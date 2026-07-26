package com.meridia.shared.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meridia.shared.auth.AuthModule
import com.meridia.shared.models.NotificationDto
import com.meridia.shared.network.NotificationRepository
import com.meridia.shared.network.NotificationRepositoryImpl
import com.meridia.shared.utils.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The notifications list (DEV-061). `Content` carries the client's own
 * notifications; the unread count drives the entry-point dot. Opening the
 * screen marks them read (markRead=true) so the dot clears on the next visit.
 */
sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Error(val message: String) : NotificationsUiState
    data class Content(val notifications: List<NotificationDto>) : NotificationsUiState {
        val unreadCount: Int get() = notifications.count { !it.isRead }
    }
}

class NotificationsViewModel(private val repo: NotificationRepository? = null) : ViewModel() {

    private val _state = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    private fun repository(): NotificationRepository =
        repo ?: NotificationRepositoryImpl(authManager = AuthModule.getAuthManager())

    /** Loads notifications; when [markRead] is set, marks any unread ones read after showing. */
    fun load(markRead: Boolean = false) {
        _state.value = NotificationsUiState.Loading
        viewModelScope.launch {
            runCatching { repository().myNotifications() }
                .onSuccess { list ->
                    _state.value = NotificationsUiState.Content(list)
                    if (markRead && list.any { !it.isRead }) {
                        runCatching { repository().markAllRead() }
                    }
                }
                .onFailure {
                    _state.value = NotificationsUiState.Error(ErrorHandler.handleHttpError(it))
                }
        }
    }
}
