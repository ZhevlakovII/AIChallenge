package ru.izhxx.aichallenge.core.network.api.config

import ru.izhxx.aichallenge.core.network.api.config.backoff.BackoffPolicy
import ru.izhxx.aichallenge.core.network.api.config.backoff.ExponentialBackoffPolicy

data class WsConfig(
    val pingIntervalMs: Long = 15_000,
    val maxFrameSizeBytes: Long = 1L shl 20, // 1 MiB
    val connectTimeoutMs: Long = 10_000,
    val backoff: BackoffPolicy = ExponentialBackoffPolicy()
)
