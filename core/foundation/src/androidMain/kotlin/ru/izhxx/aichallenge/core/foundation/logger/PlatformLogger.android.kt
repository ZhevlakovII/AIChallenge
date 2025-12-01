package ru.izhxx.aichallenge.core.foundation.logger

import android.util.Log

@PublishedApi
internal actual object PlatformLogger {

    actual fun log(level: LogLevel, tag: String, throwable: Throwable?, message: String) {
        chunkedOutput(level, tag, throwable, message)
    }

    private tailrec fun chunkedOutput(
        level: LogLevel,
        tag: String,
        throwable: Throwable?,
        message: String
    ) {
        if (message.length > LOGCAT_SAFE_LENGTH) {
            val msg = message.substring(0, LOGCAT_SAFE_LENGTH)
            val msgTail = message.substring(LOGCAT_SAFE_LENGTH)
            printToLogcat(level, tag, throwable, msg)
            chunkedOutput(level, tag, throwable, msgTail)
        } else {
            printToLogcat(level, tag, throwable, message)
        }
    }

    private fun printToLogcat(
        level: LogLevel,
        tag: String,
        throwable: Throwable?,
        message: String
    ) {
        when (level) {
            LogLevel.TRACE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
            LogLevel.NONE -> {}
        }
    }

    private const val LOGCAT_SAFE_LENGTH = 3500
}
