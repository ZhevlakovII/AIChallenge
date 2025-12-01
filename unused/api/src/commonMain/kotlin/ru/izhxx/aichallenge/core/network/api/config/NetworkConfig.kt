package ru.izhxx.aichallenge.core.network.api.config

/**
 * Общая конфигурация сети.
 */
data class NetworkConfig(
    val baseUrl: String,
    val timeouts: Timeouts = Timeouts(),
    val defaultHeaders: Map<String, String> = emptyMap(),
    val enableLogging: Boolean = false,
    val retryPolicy: RetryPolicy = RetryPolicy(),
    val ws: WsConfig = WsConfig(),
    val sse: SseConfig = SseConfig(),
    val streaming: StreamingConfig = StreamingConfig(),
    val json: JsonConfig = JsonConfig(),
)
