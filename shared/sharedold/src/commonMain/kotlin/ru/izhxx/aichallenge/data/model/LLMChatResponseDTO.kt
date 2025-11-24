package ru.izhxx.aichallenge.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO-модель ответа от API чат-модели
 */
@Serializable
data class LLMChatResponseDTO(
    @SerialName("id") val id: String,
    @SerialName("choices") val choices: List<ChoiceDTO>,
    @SerialName("created") val created: Long,
    @SerialName("model") val model: String,
    @SerialName("object") val objectName: String,
    @SerialName("usage") val usage: UsageDTO?
)
