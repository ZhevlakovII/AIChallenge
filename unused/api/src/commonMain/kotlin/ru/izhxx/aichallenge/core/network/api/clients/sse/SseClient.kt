package ru.izhxx.aichallenge.core.network.api.clients.sse

import kotlinx.coroutines.flow.Flow

/**
 * Клиент Server-Sent Events (SSE) поверх обычного HTTP GET.
 * Реализуется через чтение text/event-stream.
 */
interface SseClient {
    /**
     * Открывает SSE-подключение по относительному [path].
     * Возвращает поток событий до закрытия соединения.
     */
    fun open(
        path: String,
        headers: Map<String, String> = emptyMap(),
        lastEventId: String? = null
    ): Flow<SseEvent>
}
