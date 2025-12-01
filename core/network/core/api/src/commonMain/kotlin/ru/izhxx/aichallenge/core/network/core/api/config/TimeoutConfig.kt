package ru.izhxx.aichallenge.core.network.core.api.config

/**
 * Тайм-ауты по умолчанию для сетевых операций.
 */
data class TimeoutConfig(
    /** Таймаут установки соединения. */
    val connectTimeoutMillis: Long = 15_000L,
    /** Общий таймаут запроса (request lifecycle). */
    val requestTimeoutMillis: Long = 60_000L,
    /** Таймаут сокета (чтение/запись). */
    val socketTimeoutMillis: Long = 60_000L
) {
    companion object {
        val Default = TimeoutConfig(
            connectTimeoutMillis = 15_000L,
            requestTimeoutMillis = 60_000L,
            socketTimeoutMillis = 60_000L
        )
    }
}
