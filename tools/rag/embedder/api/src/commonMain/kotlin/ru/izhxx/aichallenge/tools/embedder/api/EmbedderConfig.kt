package ru.izhxx.aichallenge.tools.embedder.api

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class EmbedderConfig(
    val baseUrl: String,
    val apiKey: String? = null,
    val model: String,
    val dimensions: Int,
    val maxBatchSize: Int = 100,
    val timeout: Duration = 30.seconds
)