package ru.izhxx.aichallenge.core.network.api.config.backoff

import kotlin.math.pow
import kotlin.random.Random

/**
 * Экспоненциальный backoff с джиттером и ограничением.
 */
class ExponentialBackoffPolicy(
    val baseDelayMs: Long = 250,
    val maxDelayMs: Long = 30_000,
    val multiplier: Double = 2.0,
    val jitterFactor: Double = 0.3, // 0.0..1.0
) : BackoffPolicy {
    override fun delayMillis(attempt: Int): Long {
        val raw = (baseDelayMs * multiplier.pow(attempt.toDouble())).toLong()
        val capped = raw.coerceAtMost(maxDelayMs)
        if (jitterFactor <= 0.0) return capped
        val jitter = (capped * jitterFactor)
        val delta = (-jitter..jitter).random()
        return (capped + delta).coerceAtLeast(0L)
    }

    private fun ClosedRange<Double>.random(): Long =
        Random.nextDouble(start, endInclusive).toLong()
}
