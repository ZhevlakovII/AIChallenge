package ru.izhxx.aichallenge.tools.llm.completions.impl.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.izhxx.aichallenge.tools.llm.completions.impl.model.response.MessageResponseDTO

@Serializable
internal class ChoiceDTO(
    @SerialName("index") val index: Int,
    @SerialName("message") val message: MessageResponseDTO
)
