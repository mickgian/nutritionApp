package com.base.shared.network.chat

import kotlinx.serialization.Serializable

@Serializable
data class ChatRoleMessageDto(
    val role: String,          // "user" | "assistant"
    val content: String
)

@Serializable
data class ChatCompletionRequestDto(
    val messages: List<ChatRoleMessageDto>
)

@Serializable
data class ChatCompletionResponseDto(
    val messages: List<ChatRoleMessageDto>
)