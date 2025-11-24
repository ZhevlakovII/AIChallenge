package ru.izhxx.aichallenge.core.logger

/**
 * Запись лога, которую обрабатывают sinks.
 */
data class LogRecord(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
)
