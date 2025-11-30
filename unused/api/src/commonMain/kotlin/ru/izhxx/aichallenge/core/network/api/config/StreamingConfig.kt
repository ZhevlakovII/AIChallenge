package ru.izhxx.aichallenge.core.network.api.config

/**
 * Настройки потоковой обработки ответов (строки/NDJSON).
 */
data class StreamingConfig(
    /**
     * Максимальный размер одной логической строки/события при парсинге потоков (в байтах).
     * Защита от неограниченного роста буфера при неверном стриме.
     */
    val maxLineBytes: Int = 1_048_576 // 1 MiB
)
