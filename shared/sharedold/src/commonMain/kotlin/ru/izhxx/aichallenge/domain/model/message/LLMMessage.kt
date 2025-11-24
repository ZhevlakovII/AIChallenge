package ru.izhxx.aichallenge.domain.model.message

import ru.izhxx.aichallenge.domain.model.MessageRole

/**
 * Модель сообщения для взаимодействия с LLM
 * Содержит роль и текстовое содержимое
 */
data class LLMMessage(
    val role: MessageRole,
    val content: String
)
