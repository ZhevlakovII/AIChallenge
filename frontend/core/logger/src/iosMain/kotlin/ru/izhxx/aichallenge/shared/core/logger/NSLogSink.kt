package ru.izhxx.aichallenge.shared.core.logger

import platform.Foundation.NSLog
import ru.izhxx.aichallenge.core.logger.Formatter
import ru.izhxx.aichallenge.core.logger.LogRecord
import ru.izhxx.aichallenge.core.logger.LogSink

internal class NSLogSink : LogSink {
    override fun log(record: LogRecord) {
        // На iOS выводим одной строкой через NSLog
        val message = Formatter.format(record)
        NSLog("%@", message)
    }
}
