package ru.izhxx.aichallenge.core.network.api.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

data class Timeouts(
    val connectTimeout: Duration = 10_000.milliseconds,
    val requestTimeout: Duration = 30_000.milliseconds,
    val socketTimeout: Duration = 30_000.milliseconds,
)
