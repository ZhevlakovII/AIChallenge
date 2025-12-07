package ru.izhxx.aichallenge.core.foundation.error

/**
 * Тип поведения после получения ошибки.
 */
enum class ErrorRetry {
    /** Разрешено повторение операции. */
    Allowed,

    /** Запрещено повторение операции. */
    Forbidden,

    /** Нет информации о возможности повтора, решается на уровне получения ошибки. */
    Unknown
}
