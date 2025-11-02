package ru.izhxx.aichallenge.domain.model

/**
 * Модель сообщения для отображения в UI чата
 * Используется только внутри приложения и не сериализуется
 */
data class Message(
    val id: String,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
