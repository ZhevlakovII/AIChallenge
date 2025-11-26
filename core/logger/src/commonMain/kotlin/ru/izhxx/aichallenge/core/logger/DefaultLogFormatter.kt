package ru.izhxx.aichallenge.core.logger

/**
 * Легковесный форматтер по умолчанию.
 * По умолчанию добавляет время (millis от epoch) и, при наличии, информацию о потоке.
 * Стек исключения полностью может печататься в sink; здесь добавляется краткое представление.
 */
object DefaultLogFormatter : LogFormatter {
    override fun format(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        timestampMillis: Long,
        threadName: String?,
        threadId: Long?,
        enrichTimestamp: Boolean,
        enrichThread: Boolean
    ): String {
        val sb = StringBuilder(64 + message.length)
        if (enrichTimestamp) {
            sb.append('[').append(timestampMillis).append(']').append(' ')
        }
        sb.append('[').append(level.name).append(']').append(' ')
        sb.append('[').append(tag).append(']').append(' ')
        if (enrichThread && (threadName != null || threadId != null)) {
            sb.append('[')
            if (threadName != null) sb.append(threadName)
            if (threadName != null && threadId != null) sb.append(':')
            if (threadId != null) sb.append(threadId)
            sb.append(']').append(' ')
        }
        sb.append(message)
        if (throwable != null) {
            // Краткое упоминание исключения в одной строке.
            // Полный стек должен печататься sink'ом (если он это поддерживает).
            sb.append(" | ex=").append(throwable.toString())
        }
        return sb.toString()
    }
}
