package ru.izhxx.aichallenge.core.logger

/**
 * Нелокальное состояние сведено к поставщику конфигурации.
 * Logger не хранит изменяемых полей, потокобезопасен при конкурентном использовании.
 */
class Logger(
    private val getConfig: () -> LoggerConfig,
    private val tag: String
) {
    fun log(level: LogLevel, message: String, throwable: Throwable? = null) {
        if (level == LogLevel.OFF) return
        val cfg = getConfig()
        if (!level.isEnabled(cfg.minLevel)) return

        val time = cfg.timeProvider.now()
        val threadName = cfg.threadInfoProvider.currentName()
        val threadId = cfg.threadInfoProvider.currentId()

        val formatted = cfg.formatter.format(
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
            timestampMillis = time,
            threadName = threadName,
            threadId = threadId,
            enrichTimestamp = cfg.enrichTimestamp,
            enrichThread = cfg.enrichThread
        )

        try {
            cfg.sink.write(level, tag, formatted, throwable)
        } catch (t: Throwable) {
            // Не допускаем падения приложения из-за логгера
            try {
                cfg.onSinkError(t)
            } catch (_: Throwable) {
                // Игнорируем вторичную ошибку
            }
        }
    }

    // Синтаксический сахар по уровням
    fun trace(message: String, throwable: Throwable? = null) = log(LogLevel.TRACE, message, throwable)
    fun debug(message: String, throwable: Throwable? = null) = log(LogLevel.DEBUG, message, throwable)
    fun info(message: String, throwable: Throwable? = null) = log(LogLevel.INFO, message, throwable)
    fun warn(message: String, throwable: Throwable? = null) = log(LogLevel.WARN, message, throwable)
    fun error(message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, message, throwable)
    fun wtf(message: String, throwable: Throwable? = null) = log(LogLevel.ASSERT, message, throwable)

    /**
     * Измерение длительности выполнения блока. В случае исключения — логгируем как ERROR и пробрасываем дальше.
     */
    fun <T> time(name: String, level: LogLevel = LogLevel.DEBUG, block: () -> T): T {
        val cfg = getConfig()
        val start = cfg.timeProvider.now()
        return try {
            val result = block()
            val duration = cfg.timeProvider.now() - start
            log(level, "$name finished in ${duration}ms")
            result
        } catch (e: Throwable) {
            val duration = cfg.timeProvider.now() - start
            log(LogLevel.ERROR, "$name failed in ${duration}ms", e)
            throw e
        }
    }

    fun withTag(newTag: String): Logger = Logger(getConfig, newTag)
}

/**
 * Сравнение уровней: включён ли level при текущем минимальном уровне minLevel.
 */
private fun LogLevel.isEnabled(minLevel: LogLevel): Boolean {
    if (this == LogLevel.OFF) return false
    if (minLevel == LogLevel.OFF) return false
    return this.ordinal >= minLevel.ordinal
}
