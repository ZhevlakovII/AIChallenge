package ru.izhxx.aichallenge.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * DTO-модель запроса для ChatCompletion, содержащая дополнительные поля
 * для настроек API, которые не отправляются на сервер.
 */
@Serializable
data class LLMChatRequestDTO(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<ChatMessageDTO>,
    @SerialName("temperature") val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int,
    @SerialName("top_k") val topK: Int? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("min_p") val minP: Double? = null,
    @SerialName("top_a") val topA: Double? = null,
    @SerialName("seed") val seed: Long? = null,
    @SerialName("tools") val tools: List<LlmToolSchemaDTO>? = null,
    
    // Транзиентные поля, которые не включаются в сериализацию при отправке на сервер
    @Transient val apiKey: String = "",
    @Transient val apiUrl: String = "",
)
