package ru.izhxx.aichallenge.core.logger

import ru.izhxx.aichallenge.core.buildmode.isDebugBuild
import ru.izhxx.aichallenge.core.utils.strings.appendSpace

/**
 * Проверяет, разрешён ли заданный [level] для текущего типа сборки.
 *
 * Поведение:
 * - Debug-сборка: разрешены все уровни, кроме [LogLevel.NONE].
 * - Release-сборка: разрешены только [LogLevel.WARN] и [LogLevel.ERROR].
 *
 * @return true — если запись на данном уровне допустима.
 */
@PublishedApi
internal fun isAllowed(level: LogLevel): Boolean {
    return if (isDebugBuild()) {
        level != LogLevel.NONE
    } else {
        level == LogLevel.WARN || level == LogLevel.ERROR
    }
}

/**
 * Формирует финальную строку лога в виде:
 * [LEVEL][tag] message (+stacktrace).
 *
 * В release‑сборках, если указан [throwable], stacktrace добавляется новой строкой.
 * В debug‑сборках stacktrace умышленно не добавляется для снижения «шума» вывода.
 *
 * @param level Уровень логирования.
 * @param tag Тег.
 * @param throwable Причина (опционально).
 * @param message Текст сообщения.
 * @return Готовая строка лога.
 */
fun buildMessage(
    level: LogLevel,
    tag: String,
    throwable: Throwable?,
    message: String
): String {
    return StringBuilder().apply {
        clear()
        append("[")
        append(level.name)
        append("]")
        append("[")
        append(tag)
        append("]")
        appendSpace()
        append(message)
        if (throwable != null && !isDebugBuild()) {
            appendLine()
            append(throwable.stackTraceToString())
        }
    }.toString()
}
