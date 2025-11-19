package ru.izhxx.aichallenge.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для поддержки function calling в ответах ассистента.
 * OpenAI-style: message.tool_calls[].function.{name, arguments(JSON string)}
 */
@Serializable
data class FunctionCallDTO(
    @SerialName("name") val name: String,
    @SerialName("arguments") val arguments: String
)

/**
 * Обёртка вызова инструмента.
 * type обычно "function".
 */
@Serializable
data class ToolCallDTO(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String = "function",
    @SerialName("function") val function: FunctionCallDTO
)
