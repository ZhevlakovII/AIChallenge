package ru.izhxx.aichallenge.core.network.api.clients.sse

import ru.izhxx.aichallenge.core.foundation.error.AppError

/**
 * События SSE из потока text/event-stream.
 */
sealed class SseEvent {
    /**
     * SSE комментарий (строки, начинающиеся с ':').
     */
    data class Comment(val text: String) : SseEvent()

    /**
     * Переопределение рекомендуемой задержки реконнекта с сервера (поле retry:).
     */
    data class RetryMillis(val millis: Long) : SseEvent()

    /**
     * Полноценное SSE-сообщение (блок, разделённый пустой строкой).
     * Может содержать id, event и объединённую data (многострочную).
     */
    data class Message(
        val id: String?,
        val event: String?,
        val data: String
    ) : SseEvent()

    /**
     * Поток завершён (по инициативе клиента/сервера).
     */
    data object End : SseEvent()

    /**
     * Ошибка чтения/парсинга/соединения.
     */
    data class Failure(val error: AppError) : SseEvent()
}
