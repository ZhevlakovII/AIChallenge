package ru.izhxx.aichallenge.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * DTO-схемы инструментов для LLM (OpenAI-style tools).
 *
 * Пример:
 * {
 *   "type": "function",
 *   "function": {
 *     "name": "github.list_user_repos",
 *     "description": "Список публичных репозиториев указанного пользователя GitHub",
 *     "parameters": { ... JSON Schema ... }
 *   }
 * }
 */
@Serializable
data class LlmToolSchemaDTO(
    @SerialName("type") val type: String = "function",
    @SerialName("function") val function: LlmFunctionDTO
)

/**
 * Описание функции (инструмента) для LLM.
 */
@Serializable
data class LlmFunctionDTO(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String? = null,
    @SerialName("parameters") val parameters: JsonElement? = null
)
