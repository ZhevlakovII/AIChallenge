package ru.izhxx.aichallenge.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO-модель выбора (варианта ответа) от LLM
 */
@Serializable
data class ChoiceDTO(
    @SerialName("index") val index: Int,
    @SerialName("message") val message: ChatMessageDTO,
    @SerialName("finish_reason") val finishReason: String
)
