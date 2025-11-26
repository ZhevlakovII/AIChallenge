package ru.izhxx.aichallenge.core.logger

/**
 * Конфигурация логгера. Иммутабельная, безопасна для горячей замены.
 * Содержит только ссылки на интерфейсы (контракты). Реализации вынесены отдельно.
 */
data class LoggerConfig(
    val minLevel: LogLevel = LogLevel.INFO,
    val sink: LogSink = defaultPlatformSink(),
    val formatter: LogFormatter = DefaultLogFormatter,
    val timeProvider: TimeProvider = DefaultTimeProvider,
    val threadInfoProvider: ThreadInfoProvider = DefaultThreadInfoProvider,
    val enrichTimestamp: Boolean = true,
    val enrichThread: Boolean = true,
    val onSinkError: (Throwable) -> Unit = {}
)
