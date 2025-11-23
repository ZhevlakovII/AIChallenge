package ru.izhxx.aichallenge.shared.core.logger

import ru.izhxx.aichallenge.core.logger.Formatter
import ru.izhxx.aichallenge.core.logger.LogLevel
import ru.izhxx.aichallenge.core.logger.LogRecord
import ru.izhxx.aichallenge.core.logger.LogSink

@Suppress("TooManyFunctions")
internal class Slf4jSink : LogSink {
    override fun log(record: LogRecord) {
        val logger = org.slf4j.LoggerFactory.getLogger(record.tag)
        val msg = Formatter.format(record)
        val t = record.throwable

        when (record.level) {
            LogLevel.DEBUG -> {
                if (t != null) logger.debug(msg, t) else logger.debug(msg)
            }
            LogLevel.INFO -> {
                if (t != null) logger.info(msg, t) else logger.info(msg)
            }
            LogLevel.WARN -> {
                if (t != null) logger.warn(msg, t) else logger.warn(msg)
            }
            LogLevel.ERROR -> {
                if (t != null) logger.error(msg, t) else logger.error(msg)
            }
        }
    }
}
