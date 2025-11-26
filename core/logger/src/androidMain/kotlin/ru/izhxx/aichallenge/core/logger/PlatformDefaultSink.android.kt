package ru.izhxx.aichallenge.core.logger

import android.util.Log as ALog

actual fun defaultPlatformSink(): LogSink = LogSink { level, tag, message, throwable ->
    when (level) {
        LogLevel.TRACE -> if (throwable != null) ALog.v(tag, message, throwable) else ALog.v(tag, message)
        LogLevel.DEBUG -> if (throwable != null) ALog.d(tag, message, throwable) else ALog.d(tag, message)
        LogLevel.INFO  -> if (throwable != null) ALog.i(tag, message, throwable) else ALog.i(tag, message)
        LogLevel.WARN  -> if (throwable != null) ALog.w(tag, message, throwable) else ALog.w(tag, message)
        LogLevel.ERROR -> if (throwable != null) ALog.e(tag, message, throwable) else ALog.e(tag, message)
        LogLevel.ASSERT -> ALog.wtf(tag, message, throwable)
        LogLevel.OFF -> Unit
    }
}
