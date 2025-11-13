package ru.izhxx.aichallenge.features.chat.presentation.model

/**
 * Модель UI-сообщения в чате
 */
sealed class ChatUiMessage {
    abstract val id: String
    abstract val content: MessageContent
    abstract val metadata: MessageMetadata?
    
    /**
     * Сообщение пользователя
     */
    data class UserMessage(
        override val id: String,
        override val content: MessageContent,
        override val metadata: MessageMetadata? = null,
        val isHasError: Boolean
    ) : ChatUiMessage()
    
    /**
     * Сообщение от ассистента (LLM)
     */
    data class AssistantMessage(
        override val id: String,
        override val content: MessageContent,
        override val metadata: MessageMetadata? = null
    ) : ChatUiMessage()
    
    /**
     * Техническое/системное сообщение
     */
    data class TechnicalMessage(
        override val id: String,
        override val content: MessageContent,
        override val metadata: MessageMetadata? = null
    ) : ChatUiMessage()
}
