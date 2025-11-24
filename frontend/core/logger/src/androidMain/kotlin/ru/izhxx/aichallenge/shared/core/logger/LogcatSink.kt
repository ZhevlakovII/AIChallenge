wpackage ru.izhxx.aichallenge.shared.core.logger

import android.util.Log

private const val MAX_LOGCAT_LENGTH = 4000

internal class LogcatSink : LogSink {
    override fun log(record: LogRecord) {
        val tag = sanitizeTag(record.tag)
        val msg = Formatter.format(record)
        val chunks = if (msg.length <= MAX_LOGCAT_LENGTH) listOf(msg) else msg.chunked(MAX_LOGCAT_LENGTH)

        chunks.forEachIndexed { index, part ->
            val isLast = index == chunks.lastIndex
            val suffix = if (chunks.size > 1) " [${index + 1}/${chunks.size}]" else ""
            val text = part + suffix
            val t = record.throwable

            when (record.level) {
                LogLevel.DEBUG -> if (t != null && isLast) Log.d(tag, text, t) else Log.d(tag, text)
                LogLevel.INFO -> if (t != null && isLast) Log.i(tag, text, t) else Log.i(tag, text)
                LogLevel.WARN -> if (t != null && isLast) Log.w(tag, text, t) else Log.w(tag, text)
                LogLevel.ERROR -> if (t != null && isLast) Log.e(tag, text, t) else Log.e(tag, text)
            }
        }
    }

    private fun sanitizeTag(tag: String): String {
        // Ограничение Android: длина тега до 23 символов
        return if (tag.length <= 23) tag else tag.substring(0, 23)
    }
}
