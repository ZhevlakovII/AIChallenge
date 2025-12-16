package ru.izhxx.aichallenge.tools.llm.completions.impl.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class McpFunctionResponseDTO(
    @SerialName("name") val name: String,
    @SerialName("arguments") val arguments: String?,
)
