package ru.izhxx.aichallenge.tools.llm.completions.impl.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class McpToolRequestDTO(
    @SerialName("type") val type: String,
    @SerialName("function") val function: McpFunctionRequestDTO
)
