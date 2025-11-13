package ru.izhxx.aichallenge.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO-модель сообщения в чате
 */
@Serializable
data class ChatMessageDTO(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)
