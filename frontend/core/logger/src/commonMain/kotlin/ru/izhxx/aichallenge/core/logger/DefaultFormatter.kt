package ru.izhxx.aichallenge.core.logger

/**
 * Опциональный форматтер по умолчанию: добавляет timestamp и имя потока (если доступно).
 * Чтобы включить его, вызовите: LoggerConfig.enableDefaultFormatter()
 */
internal object DefaultFormatter : (LogRecord) -> String {
    override fun invoke(r: LogRecord): String {
        val ts = platformNowString()
        val thread = platformThreadName()
        val head = buildString {
            if (ts.isNotEmpty()) append("[$ts]")
            append("[${r.level.name}]")
            append("[${r.tag}]")
            if (!thread.isNullOrEmpty()) append("[$thread]")
            append(' ')
        }
        val base = head + r.message
        val t = r.throwable
        return if (t != null) base + "\n" + t.asString() else base
    }
}

/**
 * Платформенные утилиты для форматтера.
 */
internal expect fun platformNowString(): String
internal expect fun platformThreadName(): String?
