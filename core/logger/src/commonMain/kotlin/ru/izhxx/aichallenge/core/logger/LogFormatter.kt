package ru.izhxx.aichallenge.core.logger

/**
 * Форматтер отвечает за построение итоговой строковой записи лога.
 */
interface LogFormatter {
    fun format(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        timestampMillis: Long,
        threadName: String?,
        threadId: Long?,
        enrichTimestamp: Boolean,
        enrichThread: Boolean
    ): String
}
