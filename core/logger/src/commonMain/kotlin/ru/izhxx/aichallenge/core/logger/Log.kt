package ru.izhxx.aichallenge.core.logger

/**
 * Глобальная точка входа. Хранит конфигурацию как неизменяемый снимок.
 * Реализации (sink/formatter/providers) отделены и внедряются через LoggerConfig.
 */
object Log {
    @Volatile
    private var config: LoggerConfig = LoggerConfig()

    fun setConfig(newConfig: LoggerConfig) {
        // Просто атомарная замена ссылки, без мьютексов.
        config = newConfig
    }

    fun getConfig(): LoggerConfig = config

    fun tag(tag: String): Logger = Logger(::getConfig, tag)

    // Удобные короткие вызовы без явного создания Logger
    fun trace(tag: String, message: String, throwable: Throwable? = null) = tag(tag).trace(message, throwable)
    fun debug(tag: String, message: String, throwable: Throwable? = null) = tag(tag).debug(message, throwable)
    fun info(tag: String, message: String, throwable: Throwable? = null) = tag(tag).info(message, throwable)
    fun warn(tag: String, message: String, throwable: Throwable? = null) = tag(tag).warn(message, throwable)
    fun error(tag: String, message: String, throwable: Throwable? = null) = tag(tag).error(message, throwable)
    fun wtf(tag: String, message: String, throwable: Throwable? = null) = tag(tag).wtf(message, throwable)
}
