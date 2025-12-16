package ru.izhxx.aichallenge.core.logger

/**
 * Платформенный минимальный backend без форматтеров и билдеров.
 *
 * Реализации должны быть:
 * - потокобезопасными;
 * - максимально лёгкими (без тяжёлого форматирования — см. [buildMessage]);
 * - устойчивыми к исключениям (логгер не должен «ронять» приложение).
 *
 * Типичные реализации:
 * - Android: android.util.Log;
 * - JVM: System.out/System.err;
 * - iOS: NSLog/os_log/print.
 */
@PublishedApi
internal expect object PlatformLogger {

    /**
     * Записывает уже подготовленное сообщение.
     *
     * @param level Уровень логирования.
     * @param tag Короткий тег.
     * @param throwable Необязательная причина.
     * @param message Готовая строка (может быть собрана через [buildMessage]).
     *
     * Ожидание: метод не должен выполнять тяжёлое форматирование и не должен бросать исключения.
     */
    fun log(level: LogLevel, tag: String, throwable: Throwable?, message: String)
}
