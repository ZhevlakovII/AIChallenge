package ru.izhxx.aichallenge.instruments.llm.interactions.impl.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class McpFunctionRequestDTO(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("parameters") val parameters: String?,
)
