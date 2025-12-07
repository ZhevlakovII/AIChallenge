package ru.izhxx.aichallenge.instruments.llm.interactions.impl.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class MessageResponseDTO(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String?,
    @SerialName("tool_calls") val toolCalls: List<McpToolResponseDTO>?
)
