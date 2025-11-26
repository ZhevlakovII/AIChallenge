package ru.izhxx.aichallenge.core.logger

actual fun defaultPlatformSink(): LogSink = LogSink { level, _ /* tag already в сообщении */, message, throwable ->
    val stream = if (level.ordinal >= LogLevel.WARN.ordinal) System.err else System.out
    if (throwable != null) {
        // stackTraceToString доступен на JVM
        stream.println("$message\n${throwable.stackTraceToString()}")
    } else {
        stream.println(message)
    }
}
