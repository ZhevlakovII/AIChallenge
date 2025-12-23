package ru.izhxx.aichallenge.tools.llm.completions.impl.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class MessageRequestDTO(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String,
)
