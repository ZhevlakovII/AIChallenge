package ru.izhxx.aichallenge.domain.model

import kotlinx.serialization.Serializable

/**
 * Тип сообщения в чате
 */
@Serializable
enum class MessageType {
    /**
     * Техническое сообщение (приветствие, уведомления, ошибки системы)
     */
    TECHNICAL,

    /**
     * Сообщение от пользователя
     */
    USER,

    /**
     * Ответ от LLM ассистента
     */
    ASSISTANT
}
