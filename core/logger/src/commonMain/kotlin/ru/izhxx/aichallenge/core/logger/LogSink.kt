package ru.izhxx.aichallenge.core.logger

/**
 * Минимальный интерфейс приемника логов.
 * Реализация должна быть максимально быстрой и без блокировок на горячем пути.
 */
fun interface LogSink {
    fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?)
}
