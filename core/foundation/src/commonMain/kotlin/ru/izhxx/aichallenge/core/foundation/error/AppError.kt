package ru.izhxx.aichallenge.core.foundation.error

/**
 * Базовый контракт для всех ошибок приложения.
 * Содержит общие поля, набор зарезервированных ключей метаданных
 * и конкретные типы ошибок как вложенные типы для удобного API и стабильности ABI.
 *
 * @property category Категории ошибок. Определяются бизнес-требованиями
 * @property code Машино-читабельный ключ, для дополнительной муршрутизации ошибок. Примеры: "network.timeout", "http.404", "domain.user.not_found".
 * @property severity Уровень серьёзности ошибки. См. [ErrorSeverity] для возможных значений
 * @property flags Набор флагов, описывающих поведение для ошибки. См [ErrorFlag] для возможных значений
 * @property rawMessage Сырой текст ошибки
 * @property cause Оригинальная ошибка. Заполняется при наличии
 * @property metadata Дополнительные структурные атрибуты для логов и/или роутинга.
 */
// TODO(Необходимо переписать сборку ошибок, скрыть внутренние данные)
sealed interface AppError {
    val category: ErrorCategory
    val code: String
    val severity: ErrorSeverity
    val flags: Set<ErrorFlag>
    val rawMessage: String
    val cause: Throwable?
    val metadata: Map<MetadataKey, String>

    // ---- Вложенные конкретные типы ошибок ----

    /**
     * Ошибка сетевого уровня (DNS, соединение, TLS, транспорт).
     *
     * @property code Машино-читабельный ключ, для дополнительной муршрутизации ошибок. Примеры: "network.timeout", "http.404", "domain.user.not_found".
     * @property severity Уровень серьёзности ошибки. См. [ErrorSeverity] для возможных значений
     * @property flags Набор флагов, описывающих поведение для ошибки. См [ErrorFlag] для возможных значений
     * @property rawMessage Сырой текст ошибки
     * @property cause Оригинальная ошибка. Заполняется при наличии
     * @property metadata Дополнительные структурные атрибуты для логов и/или роутинга.
     *
     * @see ErrorCategory.Network
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see MetadataKeys
     */
    data class NetworkError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Network
    }

    /**
     * Ошибка уровня HTTP-протокола.
     *
     * @property code Машино-читабельный ключ, для дополнительной муршрутизации ошибок. Примеры: "network.timeout", "http.404", "domain.user.not_found".
     * @property status HTTP-статус ответа (например, 404, 500). См. [MetadataKeys.HTTP_STATUS].
     * @property method HTTP-метод запроса ("GET", "POST"). См. [MetadataKeys.METHOD].
     * @property url Полный URL запроса. См. [MetadataKeys.URL].
     * @property severity Серьёзность ошибки (влияет на обработку в UI/логике).
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [MetadataKeys]).
     *
     * @see ErrorCategory.Http
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see MetadataKeys
     */
    data class HttpError(
        override val code: String,
        val status: Int,
        val method: String,
        val url: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Http
    }

    /**
     * Ошибка сериализации/десериализации данных.
     *
     * @property code Машинный код (например, "serialization.decode_error").
     * @property severity Серьёзность ошибки.
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [MetadataKeys]).
     *
     * @see ErrorCategory.Serialization
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see MetadataKeys
     */
    data class SerializationError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Serialization
    }

    /**
     * Ошибка валидации входных данных/контрактов.
     *
     * @property code Машинный код (например, "validation.invalid_field").
     * @property severity Серьёзность ошибки.
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [MetadataKeys]).
     *
     * @see ErrorCategory.Validation
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see MetadataKeys
     */
    data class ValidationError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Validation
    }

    /**
     * Ошибка подсистемы хранения/IO (файлы, БД).
     *
     * @property code Машинный код (например, "store.write_failed").
     * @property severity Серьёзность ошибки.
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [MetadataKeys]).
     *
     * @see ErrorCategory.Storage
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see MetadataKeys
     */
    data class StorageError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Storage
    }

    /**
     * Ошибка аутентификации пользователя/сессии.
     *
     * @property code Машинный код (например, "auth.token_expired").
     * @property severity Серьёзность ошибки.
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [MetadataKeys]).
     *
     * @see ErrorCategory.Auth
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see MetadataKeys
     */
    data class AuthError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Auth
    }

    /**
     * Ошибка авторизации/прав доступа.
     *
     * @property code Машинный код (например, "permission.denied").
     * @property severity Серьёзность ошибки.
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [MetadataKeys]).
     *
     * @see ErrorCategory.Permission
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see MetadataKeys
     */
    data class PermissionError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Permission
    }

    /**
     * Ошибка превышения времени ожидания операции.
     *
     * @property code Машинный код (например, "network.timeout").
     * @property timeoutMillis Таймаут операции в миллисекундах, если известен. См. [MetadataKeys.TIMEOUT_MILLIS].
     * @property severity Серьёзность ошибки.
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [MetadataKeys]).
     *
     * @see ErrorCategory.Timeout
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see MetadataKeys
     */
    data class TimeoutError(
        override val code: String,
        val timeoutMillis: Long? = null,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Timeout
    }

    /**
     * Ошибка лимитирования запросов (rate limiting).
     *
     * @property code Машинный код (например, "rate_limit.exceeded").
     * @property retryAfterSeconds Рекомендуемая задержка перед повтором (в секундах), если известна. См. [MetadataKeys.RETRY_AFTER_SECONDS].
     * @property severity Серьёзность ошибки.
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [MetadataKeys]).
     *
     * @see ErrorCategory.RateLimit
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see MetadataKeys
     */
    data class RateLimitError(
        override val code: String,
        val retryAfterSeconds: Long? = null,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.RateLimit
    }

    /**
     * Универсальная доменная ошибка уровня бизнес-логики.
     * Рекомендуется кодировать доменные кейсы через [code] и [metadata].
     *
     * @property code Машинный код доменной ошибки (например, "domain.user.not_found").
     * @property severity Серьёзность ошибки.
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [MetadataKeys]).
     *
     * @see ErrorCategory.Domain
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see MetadataKeys
     */
    data class DomainError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Domain
    }

    /**
     * Ошибка по умолчанию для неожиданных/немаппленных ситуаций.
     *
     * @property code Машинный код (по умолчанию "unknown.unexpected").
     * @property severity Серьёзность ошибки.
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [MetadataKeys]).
     *
     * @see ErrorCategory.Unknown
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see MetadataKeys
     */
    data class UnknownError(
        override val code: String = "unknown.unexpected",
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Unknown
    }
}
