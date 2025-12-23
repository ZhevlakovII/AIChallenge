package ru.izhxx.aichallenge.tools.llm.completions.impl.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class UsageDTO(
    @SerialName("promtTokens") val promtTokens: Int,
    @SerialName("completionTokens") val completionTokens: Int,
    @SerialName("totalTokens") val totalTokens: Int,
)