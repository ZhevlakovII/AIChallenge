package ru.izhxx.aichallenge.core.network.api.clients.websocket

import ru.izhxx.aichallenge.core.foundation.error.AppError

/**
 * События WebSocket.
 */
sealed class WsEvent {
    data object Connected : WsEvent()
    data class Text(val text: String) : WsEvent()
    data class Binary(val bytes: ByteArray) : WsEvent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Binary

            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }

    data class Closed(val code: Short?, val reason: String?) : WsEvent()
    data class Failure(val error: AppError) : WsEvent()
}
