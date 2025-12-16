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

    /**
     * Начало записи голоса
     */
    object StartVoiceRecording : ChatEvent

    /**
     * Остановка записи голоса
     */
    object StopVoiceRecording : ChatEvent

    /**
     * Отмена записи голоса
     */
    object CancelVoiceRecording : ChatEvent

    /**
     * Обновление статуса разрешения на запись аудио
     */
    data class UpdatePermissionStatus(val isGranted: Boolean) : ChatEvent
}
