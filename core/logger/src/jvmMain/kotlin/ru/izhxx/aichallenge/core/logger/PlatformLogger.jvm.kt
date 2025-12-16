package ru.izhxx.aichallenge.core.logger

import ru.izhxx.aichallenge.core.logger.LogLevel
import ru.izhxx.aichallenge.core.logger.buildMessage

@PublishedApi
internal actual object PlatformLogger {

    actual fun log(level: LogLevel, tag: String, throwable: Throwable?, message: String) {
        val message = buildMessage(level, tag, throwable, message)
        when (level) {
            LogLevel.WARN, LogLevel.ERROR -> {
                System.err.println(message)
                throwable?.printStackTrace()
            }
            else -> println(message)
        }
    }
}
