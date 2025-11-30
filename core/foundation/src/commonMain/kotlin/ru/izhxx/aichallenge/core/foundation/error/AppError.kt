package ru.izhxx.aichallenge.core.foundation.error

/**
 * Базовый контракт для всех ошибок приложения.
 * Содержит общие поля, набор зарезервированных ключей метаданных
 * и конкретные типы ошибок как вложенные типы для удобного API и стабильности ABI.
 */
sealed interface AppError {
    /**
     * Категории ошибок. Стабильный набор для ABI.
     * Определяется бизнес-требованиями
     */
    val category: ErrorCategory
    /**
     * Машинно-читабельный код (ключ), используемый для дополнительной маршрутизации ошибок в UI.
     * Примеры: "network.timeout", "http.404", "domain.user.not_found".
     */
    val code: String
    /**
     * Уровень серьёзности ошибки, влияющий на обработку и сигнализацию в UI/логах.
     * См. [ErrorSeverity] для возможных значений.
     */
    val severity: ErrorSeverity
    /**
     * Набор флагов, описывающих поведение/природу ошибки.
     * Например: Transient/Retriable/Permanent.
     */
    val flags: Set<ErrorFlag>
    /**
     * Сырой текст ошибки для логов и диагностики (не локализованный).
     * Не должен содержать PII.
     */
    val rawMessage: String
    /**
     * Исходная причина (Throwable), если есть.
     * На iOS/JVM это общий KMP-Throwable.
     */
    val cause: Throwable?
    /**
     * Дополнительные структурные атрибуты (строковые), пригодны для логов и роутинга в UI.
     * Рекомендуется использовать неймспейсы ключей:
     * - "network.*", "http.*", "store.*", "domain.*", "mvi.*" и т.д.
     */
    val metadata: Map<String, String>

    /**
     * Резервированные ключи для метаданных.
     * Используйте их для единообразия по всем модулям.
     */
    object MetadataKeys {
        const val CORRELATION_ID = "correlation_id"
        const val REQUEST_ID = "request_id"
        const val OPERATION = "operation"
        const val FEATURE = "feature"
        const val MODULE = "module"

        // Network/HTTP
        const val URL = "url"
        const val ENDPOINT = "endpoint"
        const val METHOD = "method"
        const val HTTP_STATUS = "http_status"
        const val RETRY_AFTER_SECONDS = "retry_after_seconds"

        // Storage/IO
        const val FILE_PATH = "file_path"
        const val SQL_STATE = "sql_state"
        const val CONSTRAINT = "constraint"

        // Validation
        const val VALIDATION_FIELD = "validation_field"
        const val VALIDATION_RULE = "validation_rule"

        // Timeout
        const val TIMEOUT_MILLIS = "timeout_millis"

        // Origin (библиотека/слой)
        const val ORIGIN = "origin"
    }

    // ---- Вложенные конкретные типы ошибок ----

    /**
     * Ошибка сетевого уровня (DNS, соединение, TLS, транспорт).
     *
     * @property code Машинный код (например, "network.unreachable", "network.timeout").
     * @property severity Серьёзность ошибки (влияет на обработку в UI/логике).
     * @property flags Флаги поведения (см. [ErrorFlag]), напр. Transient/Retriable.
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [AppError.MetadataKeys]).
     *
     * @see ErrorCategory.Network
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see AppError.MetadataKeys
     */
    data class NetworkError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<String, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Network
    }

    /**
     * Ошибка уровня HTTP-протокола.
     *
     * @property code Машинный код (например, "http.404", "http.500").
     * @property status HTTP-статус ответа (например, 404, 500). См. [AppError.MetadataKeys.HTTP_STATUS].
     * @property method HTTP-метод запроса ("GET", "POST"). См. [AppError.MetadataKeys.METHOD].
     * @property url Полный URL запроса. См. [AppError.MetadataKeys.URL].
     * @property severity Серьёзность ошибки (влияет на обработку в UI/логике).
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [AppError.MetadataKeys]).
     *
     * @see ErrorCategory.Http
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see AppError.MetadataKeys
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
        override val metadata: Map<String, String> = emptyMap(),
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
     * @property metadata Дополнительные атрибуты (используйте [AppError.MetadataKeys]).
     *
     * @see ErrorCategory.Serialization
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see AppError.MetadataKeys
     */
    data class SerializationError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<String, String> = emptyMap(),
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
     * @property metadata Дополнительные атрибуты (используйте [AppError.MetadataKeys]).
     *
     * @see ErrorCategory.Validation
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see AppError.MetadataKeys
     */
    data class ValidationError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<String, String> = emptyMap(),
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
     * @property metadata Дополнительные атрибуты (используйте [AppError.MetadataKeys]).
     *
     * @see ErrorCategory.Storage
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see AppError.MetadataKeys
     */
    data class StorageError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<String, String> = emptyMap(),
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
     * @property metadata Дополнительные атрибуты (используйте [AppError.MetadataKeys]).
     *
     * @see ErrorCategory.Auth
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see AppError.MetadataKeys
     */
    data class AuthError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<String, String> = emptyMap(),
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
     * @property metadata Дополнительные атрибуты (используйте [AppError.MetadataKeys]).
     *
     * @see ErrorCategory.Permission
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see AppError.MetadataKeys
     */
    data class PermissionError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<String, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Permission
    }

    /**
     * Ошибка превышения времени ожидания операции.
     *
     * @property code Машинный код (например, "network.timeout").
     * @property timeoutMillis Таймаут операции в миллисекундах, если известен. См. [AppError.MetadataKeys.TIMEOUT_MILLIS].
     * @property severity Серьёзность ошибки.
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [AppError.MetadataKeys]).
     *
     * @see ErrorCategory.Timeout
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see AppError.MetadataKeys
     */
    data class TimeoutError(
        override val code: String,
        val timeoutMillis: Long? = null,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<String, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Timeout
    }

    /**
     * Ошибка лимитирования запросов (rate limiting).
     *
     * @property code Машинный код (например, "rate_limit.exceeded").
     * @property retryAfterSeconds Рекомендуемая задержка перед повтором (в секундах), если известна. См. [AppError.MetadataKeys.RETRY_AFTER_SECONDS].
     * @property severity Серьёзность ошибки.
     * @property flags Флаги поведения (см. [ErrorFlag]).
     * @property rawMessage Нелокализованное сообщение для логов/диагностики.
     * @property cause Исходная причина, если есть.
     * @property metadata Дополнительные атрибуты (используйте [AppError.MetadataKeys]).
     *
     * @see ErrorCategory.RateLimit
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see AppError.MetadataKeys
     */
    data class RateLimitError(
        override val code: String,
        val retryAfterSeconds: Long? = null,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<String, String> = emptyMap(),
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
     * @property metadata Дополнительные атрибуты (используйте [AppError.MetadataKeys]).
     *
     * @see ErrorCategory.Domain
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see AppError.MetadataKeys
     */
    data class DomainError(
        override val code: String,
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<String, String> = emptyMap(),
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
     * @property metadata Дополнительные атрибуты (используйте [AppError.MetadataKeys]).
     *
     * @see ErrorCategory.Unknown
     * @see ErrorSeverity
     * @see ErrorFlag
     * @see AppError.MetadataKeys
     */
    data class UnknownError(
        override val code: String = "unknown.unexpected",
        override val severity: ErrorSeverity = ErrorSeverity.Error,
        override val flags: Set<ErrorFlag> = emptySet(),
        override val rawMessage: String = "",
        override val cause: Throwable? = null,
        override val metadata: Map<String, String> = emptyMap(),
    ) : AppError {
        override val category: ErrorCategory = ErrorCategory.Unknown
    }
}
