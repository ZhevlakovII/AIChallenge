package ru.izhxx.aichallenge.core.network.api.config.backoff

/**
 * Политика вычисления задержки между повторами (для SSE/WS и прочего).
 */
fun interface BackoffPolicy {
    /**
     * @param attempt попытка реконнекта/повтора (0..N)
     * @return задержка в миллисекундах перед следующей попыткой
     */
    fun delayMillis(attempt: Int): Long
}
