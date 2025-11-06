package ru.izhxx.aichallenge.domain.model

import kotlinx.serialization.Serializable

/**
 * Модель сообщения для отображения в UI чата
 * Содержит информацию о типе сообщения и формате ответа для сообщений от LLM
 */
@Serializable
data class Message(
    val id: String,
    val text: String,
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis(),
    val responseFormat: ResponseFormat? = null
) {
    /**
     * Вспомогательное свойство для обратной совместимости
     */
    val isFromUser: Boolean
        get() = type == MessageType.USER
}
