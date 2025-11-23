package ru.izhxx.aichallenge.core.logger

/**
 * Универсальный мультиплатформенный логгер.
 * Требования:
 * - Только уровни D/I/W/E
 * - Минимальный уровень по умолчанию: INFO (см. LoggerConfig)
 * - Явные теги (без автотегов)
 * - Возможность runtime-переключения уровня (LoggerConfig.minLevel)
 *
 * По умолчанию при первом использовании подключаются платформенные sinks (см. PlatformDefaults).
 */
class Logger(val tag: String) {

    // region Public API: строки
    fun d(message: String) = log(LogLevel.DEBUG, message, null)
    fun i(message: String) = log(LogLevel.INFO, message, null)
    fun w(message: String, throwable: Throwable? = null) = log(LogLevel.WARN, message, throwable)
    fun e(message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, message, throwable)
    // endregion

    // region Public API: ленивые сообщения
    inline fun d(message: () -> String) {
        if (LoggerConfig.isEnabled(LogLevel.DEBUG)) log(LogLevel.DEBUG, message(), null)
    }
    inline fun i(message: () -> String) {
        if (LoggerConfig.isEnabled(LogLevel.INFO)) log(LogLevel.INFO, message(), null)
    }
    inline fun w(message: () -> String, throwable: Throwable? = null) {
        if (LoggerConfig.isEnabled(LogLevel.WARN)) log(LogLevel.WARN, message(), throwable)
    }
    inline fun e(message: () -> String, throwable: Throwable? = null) {
        if (LoggerConfig.isEnabled(LogLevel.ERROR)) log(LogLevel.ERROR, message(), throwable)
    }
    // endregion

    fun withTag(suffix: String): Logger = Logger("$tag:$suffix")

    fun log(level: LogLevel, message: String, throwable: Throwable? = null) {
        if (!LoggerConfig.isEnabled(level)) return
        // Обеспечиваем наличие дефолтных sinks
        LoggerConfig.ensureDefaults()

        val record = LogRecord(level = level, tag = tag, message = message, throwable = throwable)
        // Снимок sinks и публикация
        val sinks = LoggerConfig.sinks
        for (sink in sinks) {
            runCatching { sink.log(record) }
                .onFailure { e -> LoggerConfig.onSinkError?.invoke(sink, record, e) }
        }
    }

    companion object {
        /**
         * Создает Logger с тегом по имени класса (явный тег из simpleName, без автоанализа стека).
         */
        fun forClass(owner: Any): Logger {
            val tag = owner::class.simpleName ?: "Unknown"
            return Logger(tag)
        }
    }
}
