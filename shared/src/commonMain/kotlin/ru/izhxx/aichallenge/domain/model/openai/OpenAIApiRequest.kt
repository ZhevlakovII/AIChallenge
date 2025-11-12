package ru.izhxx.aichallenge.domain.model.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Расширенная модель запроса для ChatCompletion, содержащая дополнительные поля
 * для настроек API, которые не отправляются на сервер.
 */
@Serializable
data class LLMChatRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<ChatMessage>,
    @SerialName("temperature") val temperature: Double,
    @SerialName("max_tokens") val maxTokens: Int,
    
    // Транзиентные поля, которые не включаются в сериализацию при отправке на сервер
    @Transient val apiKey: String = "",
    @Transient val apiUrl: String = "",
    @Transient val openAIProject: String = ""
)

@Serializable
data class LLMChatResponse(
    @SerialName("id") val id: String,
    @SerialName("choices") val choices: List<Choice>,
    @SerialName("created") val created: Long,
    @SerialName("model") val model: String,
    @SerialName("object") val objectName: String,
    @SerialName("usage") val usage: Usage? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int,
    @SerialName("completion_tokens") val completionTokens: Int,
    @SerialName("total_tokens") val totalTokens: Int
)

@Serializable
data class ChatMessage(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

@Serializable
data class Choice(
    @SerialName("index") val index: Int,
    @SerialName("message") val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String
)
