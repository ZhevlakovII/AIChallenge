package ru.izhxx.aichallenge.core.logger

/**
 * Поддерживаемые уровни логирования (по требованию: только D/I/W/E).
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR
}

internal fun LogLevel.priority(): Int = when (this) {
    LogLevel.DEBUG -> 10
    LogLevel.INFO -> 20
    LogLevel.WARN -> 30
    LogLevel.ERROR -> 40
}
