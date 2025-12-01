package ru.izhxx.aichallenge.core.network.api.clients.websocket

import kotlinx.coroutines.flow.Flow

/**
 * Минимальный WebSocket-клиент.
 * Предоставляет поток входящих событий и предполагает авто‑реконнект на уровне реализации.
 */
interface WebSocketClient {
    /**
     * Подключается к [path] и эмитит события. При потере соединения реализация может
     * переподключаться согласно политике бэкоффа.
     */
    fun stream(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): Flow<WsEvent>
}
