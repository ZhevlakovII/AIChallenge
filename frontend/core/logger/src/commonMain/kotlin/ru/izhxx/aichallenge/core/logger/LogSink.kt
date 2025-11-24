package ru.izhxx.aichallenge.core.logger

/**
 * Получатель логов. Можно подключать несколько sink'ов.
 */
interface LogSink {
    fun log(record: LogRecord)
}
