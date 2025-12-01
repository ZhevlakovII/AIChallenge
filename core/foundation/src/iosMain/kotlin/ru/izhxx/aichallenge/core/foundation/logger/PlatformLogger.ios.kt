package ru.izhxx.aichallenge.core.foundation.logger

import platform.Foundation.NSLog

@PublishedApi
internal actual object PlatformLogger {

    actual fun log(level: LogLevel, tag: String, throwable: Throwable?, message: String) {
        NSLog(
            format = "%@",
            buildMessage(level, tag, throwable, message)
        )
    }
}
