package ru.izhxx.aichallenge.instruments.llm.interactions.impl.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class  McpToolResponseDTO(
    @SerialName("type") val type: String,
    @SerialName("function") val function: McpFunctionResponseDTO
)
