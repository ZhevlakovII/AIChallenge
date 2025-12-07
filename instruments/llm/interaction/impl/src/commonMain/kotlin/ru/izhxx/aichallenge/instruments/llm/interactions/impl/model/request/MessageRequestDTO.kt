package ru.izhxx.aichallenge.instruments.llm.interactions.impl.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class MessageRequestDTO(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String,
)
