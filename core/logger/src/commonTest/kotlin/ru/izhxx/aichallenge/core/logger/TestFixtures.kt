package ru.izhxx.aichallenge.core.logger

data class LogRecord(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable?
)

/**
 * Тестовый sink, собирающий записи для последующих проверок.
 */
class CollectingSink : LogSink {
    val records: MutableList<LogRecord> = mutableListOf()
    override fun write(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        records.add(LogRecord(level, tag, message, throwable))
    }
}

/**
 * Провайдер времени, возвращающий возрастающее значение при каждом вызове.
 * Удобно для тестирования time() и порядка вызовов.
 */
class SteppingTimeProvider(
    start: Long = 0L,
    private val step: Long = 1L
) : TimeProvider {
    private var current: Long = start
    override fun now(): Long {
        val value = current
        current += step
        return value
    }
}

/**
 * Константный провайдер информации о "потоке".
 */
class ConstThreadInfoProvider(
    private val name: String? = null,
    private val id: Long? = null
) : ThreadInfoProvider {
    override fun currentName(): String? = name
    override fun currentId(): Long? = id
}
