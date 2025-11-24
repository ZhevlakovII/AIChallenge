package ru.izhxx.aichallenge.core.logger

internal object Formatter {
    fun format(record: LogRecord): String {
        // Позволяем переопределить формат через LoggerConfig.formatter
        val custom = LoggerConfig.formatter
        if (custom != null) {
            return custom(record)
        }

        val base = "[${record.level.name}][${record.tag}] ${record.message}"
        val thr = record.throwable
        return if (thr != null) {
            // stackTraceToString есть на JVM; на других платформах может дать краткую форму
            val trace = thr.asString()
            "$base\n$trace"
        } else {
            base
        }
    }
}

internal class ConsoleSink : LogSink {
    override fun log(record: LogRecord) {
        // Простая печать в stdout
        println(Formatter.format(record))
    }
}
