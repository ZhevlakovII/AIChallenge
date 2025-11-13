package ru.izhxx.aichallenge.features.chat.presentation.model

/**
 * События для фичи чата в паттерне MVI
 */
sealed interface ChatEvent {
    /**
     * Отправка сообщения
     */
    data class SendMessage(val text: String) : ChatEvent
    
    /**
     * Повторная отправка последнего сообщения
     */
    object RetryLastMessage : ChatEvent
    
    /**
     * Очистка истории чата
     */
    object ClearHistory : ChatEvent
    
    /**
     * Обновление текста в поле ввода
     */
    data class UpdateInputText(val text: String) : ChatEvent
}
