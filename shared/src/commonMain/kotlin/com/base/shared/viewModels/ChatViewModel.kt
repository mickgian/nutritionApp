package com.base.shared.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.base.shared.models.ChatMessage
import com.base.shared.models.ChatUiState
import com.base.shared.models.SessionResponse
import com.base.shared.network.auth.SessionRepository
import com.base.shared.network.chat.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repo: ChatRepository,          // ↖ talk to /chatbot/chat
    private val sessionRepo: SessionRepository, // ↖ should you need CRUD ops
    private val initialSession: SessionResponse, // ↖ gives us sessionId & token
    private val onSessionUpdated: (() -> Unit)? = null // ↖ callback to refresh session list
) : ViewModel() {

    private val _state = MutableStateFlow<ChatUiState>(
        ChatUiState.Idle
    )
    val state: StateFlow<ChatUiState> = _state

    /** Local rolling buffer rendered by the UI */
    private val buffer = mutableListOf<ChatMessage>()

    /** Always valid: taken from `initialSession` or a freshly-created one */
    private var sessionId: String = initialSession.sessionId
    
    init {
        // Load existing chat history for this session
        loadChatHistory()
    }
    
    /** Load existing chat history from the backend */
    private fun loadChatHistory() = viewModelScope.launch {
        _state.value = ChatUiState.Loading
        runCatching { 
            repo.loadChatHistory(sessionId) 
        }.onSuccess { messages ->
            buffer.clear()
            buffer.addAll(messages)
            // Show "Start a conversation" text if no messages, otherwise show history
            _state.value = if (messages.isEmpty()) {
                ChatUiState.Idle
            } else {
                ChatUiState.History(buffer.toList())
            }
        }.onFailure { e ->
            // If loading fails, start with Idle state to show "Start a conversation"
            buffer.clear()
            _state.value = ChatUiState.Idle
            println("KMP_LOG: [ChatViewModel] Failed to load chat history: ${e.message}")
        }
    }

    /** Send a user message → optimistic UI → backend → assistant reply   */
    fun sendMessage(userText: String) = viewModelScope.launch {
        // optimistic append
        buffer += ChatMessage(text = userText, fromUser = true)
        _state.value = ChatUiState.History(buffer.toList())

        // If this is the first message, generate a title from it
        val isFirstMessage = buffer.size == 1
        
        // remote call
        _state.value = ChatUiState.Loading
        runCatching { repo.sendMessage(sessionId, userText) }
            .onSuccess { assistantText ->
                buffer += ChatMessage(text = assistantText, fromUser = false)
                _state.value = ChatUiState.History(buffer.toList())
                
                // Auto-rename session based on first user message
                if (isFirstMessage) {
                    val title = generateTitleFromMessage(userText)
                    runCatching { 
                        // Create session-specific repository for rename operation
                        val sessionSpecificRepo = com.base.shared.network.auth.SessionRepositoryImpl(initialSession.token.accessToken)
                        sessionSpecificRepo.renameSession(sessionId, title)
                    }.onSuccess {
                        // Notify that session was updated so the UI can refresh
                        onSessionUpdated?.invoke()
                    }.onFailure { error ->
                        println("KMP_LOG: [DEBUG] Auto-rename failed: ${error.message}")
                        // Don't fail the whole chat operation if rename fails
                    }
                }
            }
            .onFailure { e ->
                _state.value = ChatUiState.Error(e.message ?: "Send failed")
            }
    }
    
    private fun generateTitleFromMessage(message: String): String {
        // Smart title extraction: extract key meaningful words
        val cleanMessage = message.trim()
        
        // Remove common question words from the beginning
        val questionWords = listOf("how", "what", "where", "when", "why", "who", "can", "could", "would", "should", "is", "are", "do", "does")
        val words = cleanMessage.split("\\s+".toRegex())
        
        // Find the first meaningful part after question words
        val meaningfulStart = words.indexOfFirst { word ->
            !questionWords.contains(word.lowercase()) && word.length > 2
        }.let { if (it == -1) 0 else maxOf(0, it - 1) }
        
        val relevantWords = words.drop(meaningfulStart)
        
        // Build title with key words
        val title = relevantWords.take(8).joinToString(" ")
        
        return if (title.length <= 50) {
            title.replaceFirstChar { it.uppercaseChar() }
        } else {
            title.take(47) + "..."
        }
    }

    /** Optional helper: start an **empty** brand-new session */
    suspend fun startNewSession() {
        _state.value = ChatUiState.Loading
        val new = sessionRepo.createSession()
        sessionId = new.sessionId
        buffer.clear()
        _state.value = ChatUiState.Idle
    }
}
