package ru.izhxx.aichallenge.core.network.api.config

data class RetryPolicy(
    val retries: Int = 3,
    val baseDelayMs: Long = 200,
    val maxDelayMs: Long = 2_000,
    val jitterFactor: Double = 0.3, // 0.0..1.0
)
