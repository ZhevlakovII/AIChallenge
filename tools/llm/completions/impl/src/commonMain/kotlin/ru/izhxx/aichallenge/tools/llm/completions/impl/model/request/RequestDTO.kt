package ru.izhxx.aichallenge.tools.llm.completions.impl.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class RequestDTO(
    @SerialName("messages") val messages: List<MessageRequestDTO>,
    @SerialName("model") val model: String,
    @SerialName("temperature") val temperature: Double,
    @SerialName("maxTokens") val maxTokens: Int,
    @SerialName("topK") val topK: Int,
    @SerialName("topP") val topP: Double,
    @SerialName("minP") val minP: Double,
    @SerialName("topA") val topA: Double,
    @SerialName("seed") val seed: Long,
    @SerialName("tools") val tools: List<McpToolRequestDTO>?,
)