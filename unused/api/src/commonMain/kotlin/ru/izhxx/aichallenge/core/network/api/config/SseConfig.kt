package ru.izhxx.aichallenge.core.network.api.config

import ru.izhxx.aichallenge.core.network.api.config.backoff.BackoffPolicy
import ru.izhxx.aichallenge.core.network.api.config.backoff.ExponentialBackoffPolicy

data class SseConfig(
    val defaultRetryMs: Long = 1_000,
    val backoff: BackoffPolicy = ExponentialBackoffPolicy()
)
