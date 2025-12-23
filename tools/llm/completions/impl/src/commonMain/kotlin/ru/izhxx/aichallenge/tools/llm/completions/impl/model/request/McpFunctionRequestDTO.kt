package ru.izhxx.aichallenge.tools.llm.completions.impl.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class McpFunctionRequestDTO(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("parameters") val parameters: String?,
)
