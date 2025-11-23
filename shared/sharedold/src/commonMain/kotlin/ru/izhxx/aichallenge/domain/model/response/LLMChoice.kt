package ru.izhxx.aichallenge.domain.model.response

import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.message.ParsedMessage

/**
 * Модель выбора (варианта ответа) от LLM
 */
data class LLMChoice(
    val index: Int,
    val rawMessage: LLMMessage,
    val parsedMessage: ParsedMessage?,
    val finishReason: String
)
