package ru.izhxx.aichallenge.instruments.llm.interactions.impl.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class McpToolRequestDTO(
    @SerialName("type") val type: String,
    @SerialName("function") val function: McpFunctionRequestDTO
)
