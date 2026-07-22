package com.base.shared.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class ChatMessage(
    val uuid: String = Uuid.random().toString(),
    val text: String,
    val fromUser: Boolean,
    val timestamp: Instant = Clock.System.now()
)

sealed interface ChatUiState {
    object Idle : ChatUiState
    object Loading : ChatUiState
    data class Error(val message: String) : ChatUiState
    data class History(val messages: List<ChatMessage>) : ChatUiState
}