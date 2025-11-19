package ru.izhxx.aichallenge.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO-модель сообщения в чате
 *
 * Поддерживает function calling:
 * - tool_calls: список вызовов инструментов в сообщении ассистента
 * - tool_call_id: идентификатор вызова инструмента для сообщения role="tool"
 */
@Serializable
data class ChatMessageDTO(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCallDTO>? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null
)
