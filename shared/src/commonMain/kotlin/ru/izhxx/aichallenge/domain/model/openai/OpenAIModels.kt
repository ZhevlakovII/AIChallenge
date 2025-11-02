package ru.izhxx.aichallenge.domain.model.openai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    @SerialName("model") val model: String,
    @SerialName("messages") val messages: List<ChatMessage>,
    @SerialName("temperature") val temperature: Double
)

@Serializable
data class ChatCompletionResponse(
    @SerialName("id") val id: String,
    @SerialName("choices") val choices: List<Choice>,
    @SerialName("created") val created: Long,
    @SerialName("model") val model: String,
    @SerialName("object") val objectName: String
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
