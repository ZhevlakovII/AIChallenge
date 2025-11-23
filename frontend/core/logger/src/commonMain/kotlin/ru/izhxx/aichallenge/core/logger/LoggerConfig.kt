package ru.izhxx.aichallenge.core.logger

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

/**
 * Глобальная конфигурация логгера.
 * Требования:
 * - Только уровни D/I/W/E
 * - Минимальный уровень по умолчанию: INFO
 * - Явные теги (без автотегов)
 * - Возможность runtime-переключения уровня
 */
object LoggerConfig {
    // Уровень логирования (lock-free, мультиплатформенно)
    private val minLevelRef = atomic(LogLevel.INFO)
    var minLevel: LogLevel
        get() = minLevelRef.value
        set(value) { minLevelRef.value = value }

    /**
     * Проверка включённости уровня без зависимости от ordinal.
     */
    fun isEnabled(level: LogLevel): Boolean = level.priority() >= minLevelRef.value.priority()

    /**
     * Управление автоустановкой дефолтных sinks.
     */
    private val autoInstallDefaultsRef = atomic(true)
    var autoInstallDefaults: Boolean
        get() = autoInstallDefaultsRef.value
        set(value) { autoInstallDefaultsRef.value = value }

    /**
     * Хук для форматирования и ошибок sink'ов.
     * Если formatter == null — используется встроенный Formatter.format(record).
     */
    var formatter: ((LogRecord) -> String)? = null

    fun enableDefaultFormatter() {
        formatter = DefaultFormatter
    }

    var onSinkError: ((sink: LogSink, record: LogRecord, error: Throwable) -> Unit)? = null

    /**
     * Copy-on-write снапшот sinks с lock-free обновлением через atomicfu.
     */
    private val sinksRef = atomic<List<LogSink>>(emptyList())
    val sinks: List<LogSink> get() = sinksRef.value

    fun addSink(sink: LogSink) {
        sinksRef.update { list ->
            if (list.any { it::class == sink::class }) list else list + sink
        }
    }

    fun removeSink(sink: LogSink) {
        sinksRef.update { list -> list - sink }
    }

    fun addSinks(vararg sinks: LogSink) {
        sinksRef.update { list ->
            var res = list
            for (s in sinks) {
                if (res.none { it::class == s::class }) res = res + s
            }
            res
        }
    }

    fun setSinks(vararg sinks: LogSink) {
        // Установка нового набора (уникальность по типу)
        val unique = buildList {
            sinks.forEach { s ->
                if (this.none { it::class == s::class }) add(s)
            }
        }
        sinksRef.value = unique
    }

    fun clearSinks() {
        sinksRef.value = emptyList()
    }

    // Одноразовая установка дефолтных sinks (guarded CAS)
    private val defaultsInstalledRef = atomic(false)

    internal fun ensureDefaults() {
        if (!autoInstallDefaultsRef.value) return
        if (sinksRef.value.isNotEmpty()) return
        if (defaultsInstalledRef.compareAndSet(expect = false, update = true)) {
            PlatformDefaults.installDefaultSinks()
        }
    }

    fun installDefaults() {
        if (defaultsInstalledRef.compareAndSet(expect = false, update = true)) {
            PlatformDefaults.installDefaultSinks()
        }
    }
}

/**
 * Платформенно-специфичная установка дефолтных sinks.
 * Android: Logcat
 * JVM (Desktop/Backend): SLF4J (logback), иначе Console fallback
 */
expect object PlatformDefaults {
    fun installDefaultSinks()
}
