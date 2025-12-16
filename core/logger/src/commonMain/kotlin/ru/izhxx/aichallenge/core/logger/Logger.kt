@file:OptIn(ExperimentalContracts::class)

package ru.izhxx.aichallenge.core.logger

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Универсальная точка логирования.
 *
 * Выполняет быстрый guard через [isAllowed] и вычисляет [message] лениво.
 * Контракт гарантирует, что [message] будет вызван не более одного раза.
 *
 * Параметры:
 * - [level] целевой уровень логирования.
 * - [tag] короткий тег (см. [Tag]); рекомендуется формат "Feature/Module".
 * - [throwable] необязательная причина; в релиз‑сборках стек будет добавлен к сообщению.
 * - [message] лямбда, формирующая строку; не вызывается, если уровень запрещён.
 *
 * Пример:
 * ```kotlin
 * debug(Tag.of("Auth")) { "User=$userId logged in" }
 * ```
 *
 * Замечания:
 * - В debug‑сборках разрешены все уровни кроме [LogLevel.NONE]; в release — только [LogLevel.WARN] и [LogLevel.ERROR].
 * - Избегайте PII: персональные данные, токены и т.п.
 */
inline fun log(
    level: LogLevel,
    tag: Tag,
    throwable: Throwable? = null,
    message: () -> String
) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (!isAllowed(level)) return
    PlatformLogger.log(level, tag.value, throwable, message())
}

/**
 * Трассировочные события (самый подробный уровень).
 * Вызывается лениво, вычисление [message] произойдёт только при разрешённом уровне.
 *
 * Замечание: в release‑сборках [LogLevel.TRACE] игнорируется.
 */
inline fun trace(tag: Tag, message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (!isAllowed(LogLevel.TRACE)) return
    PlatformLogger.log(LogLevel.TRACE, tag.value, null, message())
}

/**
 * Отладочные события.
 * Вызывается лениво, вычисление [message] произойдёт только при разрешённом уровне.
 *
 * Замечание: в release‑сборках [LogLevel.DEBUG] игнорируется.
 */
inline fun debug(tag: Tag, message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (!isAllowed(LogLevel.DEBUG)) return
    PlatformLogger.log(LogLevel.DEBUG, tag.value, null, message())
}

/**
 * Предупреждение о потенциальной проблеме.
 *
 * Если указан [throwable], то в release‑сборках его stacktrace будет добавлен к сообщению.
 */
inline fun warn(tag: Tag, throwable: Throwable? = null, message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (!isAllowed(LogLevel.WARN)) return
    PlatformLogger.log(LogLevel.WARN, tag.value, throwable, message())
}

/**
 * Ошибка выполнения.
 *
 * Если указан [throwable], то в release‑сборках его stacktrace будет добавлен к сообщению.
 */
inline fun error(tag: Tag, throwable: Throwable? = null, message: () -> String) {
    contract { callsInPlace(message, InvocationKind.AT_MOST_ONCE) }
    if (!isAllowed(LogLevel.ERROR)) return
    PlatformLogger.log(LogLevel.ERROR, tag.value, throwable, message())
}
