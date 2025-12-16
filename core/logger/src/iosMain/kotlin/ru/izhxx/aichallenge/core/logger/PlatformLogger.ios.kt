package ru.izhxx.aichallenge.core.logger

import platform.Foundation.NSLog
import ru.izhxx.aichallenge.core.logger.LogLevel
import ru.izhxx.aichallenge.core.logger.buildMessage

@PublishedApi
internal actual object PlatformLogger {

    actual fun log(level: LogLevel, tag: String, throwable: Throwable?, message: String) {
        NSLog(
            format = "%@",
            buildMessage(level, tag, throwable, message)
        )
    }
}
