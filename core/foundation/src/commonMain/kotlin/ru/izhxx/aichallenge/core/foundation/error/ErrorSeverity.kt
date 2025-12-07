package ru.izhxx.aichallenge.core.foundation.error

/**
 * Уровень серьёзности ошибки.
 */
enum class ErrorSeverity {

    /** Предупреждение, возможны незначительные проблемы. */
    Warning,

    /** Ошибка, операция не выполнена. */
    Error,

    /** Критический сбой, возможна деградация функционала. */
    Critical
}
