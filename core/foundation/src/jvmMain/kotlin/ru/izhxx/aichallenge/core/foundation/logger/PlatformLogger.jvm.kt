package ru.izhxx.aichallenge.core.foundation.logger

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
