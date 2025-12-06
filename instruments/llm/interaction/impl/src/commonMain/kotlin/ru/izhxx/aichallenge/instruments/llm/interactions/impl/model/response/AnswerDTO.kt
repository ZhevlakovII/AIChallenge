package ru.izhxx.aichallenge.instruments.llm.interactions.impl.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.izhxx.aichallenge.instruments.llm.interactions.impl.model.request.ChoiceDTO

@Serializable
internal class AnswerDTO(
    @SerialName("id") val id: String,
    @SerialName("created") val createdAt: Long,
    @SerialName("model") val model: String,
    @SerialName("choices") val choices: List<ChoiceDTO>,
    @SerialName("usage") val usage: UsageDTO?
)