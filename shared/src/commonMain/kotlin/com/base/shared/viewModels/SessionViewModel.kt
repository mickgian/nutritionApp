// shared/viewModels/SessionViewModel.kt
package com.base.shared.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.base.shared.models.SessionResponse
import com.base.shared.network.auth.SessionRepository
import com.base.shared.network.auth.SessionRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SessionViewModel(
    /** JWT access-token obtained from LoginViewModel.Success */
    token: String,
    repoOverride: SessionRepository? = null
) : ViewModel() {

    private val repo: SessionRepository =
        repoOverride ?: SessionRepositoryImpl(token)

    sealed interface State {
        object Loading : State
        data class Ready(val sessions: List<SessionResponse>) : State
        data class Error(val message: String) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state

    init { reload() }

    /** Refresh from server */
    fun reload() = viewModelScope.launch(Dispatchers.Main) {
        _state.value = State.Loading
        runCatching { repo.listSessions() }
            .onSuccess { _state.value = State.Ready(it) }
            .onFailure { _state.value = State.Error(it.message ?: "Load failed") }
    }

    /** Create a fresh session and immediately add it to the list */
    suspend fun createAndUseNewSession(): SessionResponse {
        val s = repo.createSession()
        reload()           // update UI
        return s
    }
    
    /** Rename a session and update the UI */
    suspend fun renameSession(sessionId: String, newName: String, sessionToken: String) {
        // Optimistic update - update UI immediately to prevent flicker
        val currentState = _state.value
        if (currentState is State.Ready) {
            val updatedSessions = currentState.sessions.map { session ->
                if (session.sessionId == sessionId) {
                    session.copy(name = newName)
                } else {
                    session
                }
            }
            _state.value = State.Ready(updatedSessions)
        }
        
        // Then perform the actual API call
        runCatching { 
            // Create a session repository with the session-specific token for this operation
            val sessionSpecificRepo = SessionRepositoryImpl(sessionToken)
            sessionSpecificRepo.renameSession(sessionId, newName)
        }.onSuccess {
            // Success - the UI is already updated, no need to reload
            println("KMP_LOG: [DEBUG] Session renamed successfully, UI already updated")
        }.onFailure { error ->
            // Revert the optimistic update and show error
            reload() // restore original state
            _state.value = State.Error("Failed to rename session: ${error.message}")
        }
    }
}
