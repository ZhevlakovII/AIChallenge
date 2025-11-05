package ru.izhxx.aichallenge.domain.model

import kotlinx.serialization.Serializable

/**
 * Модель сообщения для отображения в UI чата
 * Содержит информацию о формате ответа для сообщений от LLM
 */
@Serializable
data class Message(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val responseFormat: ResponseFormat? = null
)
