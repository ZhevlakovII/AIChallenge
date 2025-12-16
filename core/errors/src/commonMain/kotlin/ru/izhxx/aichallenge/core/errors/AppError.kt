package ru.izhxx.aichallenge.core.errors

/**
 * Единый класс для ошибок в проекте. Позволяет конвертировать различные типы [Throwable] в конкретные типы.
 *
 * @property severity Уровень ошибки.
 * @property retry Поведение после получения ошибки.
 * @property rawMessage Исходное сообщение из [Throwable].
 * @property cause Оригинальный [Throwable].
 * @property metadata дополнительные данные, связанные с ошибкой: место вызова, сервис и прочие данные.
 *
 * @see ErrorSeverity
 * @see ErrorRetry
 * @see MetadataKey
 * @see Throwable
 */
// TODO(Дополнить документацию по ошибкам)
sealed class AppError(
    val severity: ErrorSeverity,
    val retry: ErrorRetry,
    val cause: Throwable? = null,
    val rawMessage: String? = cause?.message,
    val metadata: Map<MetadataKey, String> = emptyMap()
) {
    /* Дальше идут конкретные типы ошибок */

    /* Ошибки сетевого уровня */

    object NetworkUnavailable : AppError(
        severity = ErrorSeverity.Critical,
        retry = ErrorRetry.Allowed
    )

    object NetworkTimeout : AppError(
        severity = ErrorSeverity.Error,
        retry = ErrorRetry.Allowed
    )

    class NetworkSecurity(
        cause: Throwable? = null,
        rawMessage: String? = cause?.message,
        metadata: Map<MetadataKey, String> = emptyMap()
    ) : AppError(
        severity = ErrorSeverity.Critical,
        retry = ErrorRetry.Forbidden,
        cause = cause,
        rawMessage = rawMessage,
        metadata = metadata
    )

    class HttpError<M : Any>(
        val code: Int,
        val parsedMessage: M? = null,
        cause: Throwable? = null,
        rawMessage: String? = cause?.message,
        metadata: Map<MetadataKey, String> = emptyMap()
    ) : AppError(
        severity = ErrorSeverity.Error,
        retry = when (code) {
            429 -> ErrorRetry.Allowed
            in 500..599 -> ErrorRetry.Allowed
            else -> ErrorRetry.Forbidden
        },
        cause = cause,
        rawMessage = rawMessage,
        metadata = buildMap(metadata.size + 2) {
            put(MetadataKey("code"), code.toString())
            putAll(metadata)
        }
    )

    /* Ошибки авторизации */
    object TokenExpired : AppError(
        severity = ErrorSeverity.Critical,
        retry = ErrorRetry.Allowed,
    )

    object InvalidCredentials : AppError(
        severity = ErrorSeverity.Warning,
        retry = ErrorRetry.Forbidden,
    )

    object Unauthorized : AppError(
        severity = ErrorSeverity.Critical,
        retry = ErrorRetry.Forbidden,
    )

    object ForbiddenPermissions : AppError(
        severity = ErrorSeverity.Critical,
        retry = ErrorRetry.Forbidden,
    )

    /* Ошибки, связанные с файловой системой */
    object FileNotFound : AppError(
        severity = ErrorSeverity.Critical,
        retry = ErrorRetry.Forbidden,
    )

    class Storage(
        val path: String?,
        cause: Throwable? = null,
        rawMessage: String? = cause?.message,
        metadata: Map<MetadataKey, String> = emptyMap()
    ) : AppError(
        severity = ErrorSeverity.Error,
        retry = ErrorRetry.Forbidden,
        cause = cause,
        rawMessage = rawMessage,
        metadata = metadata,
    )

    /* Сериализация и валидация */
    class SerializationError(
        cause: Throwable? = null,
        rawMessage: String? = cause?.message,
        metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError(
        severity = ErrorSeverity.Error,
        retry = ErrorRetry.Forbidden,
        cause = cause,
        rawMessage = rawMessage,
        metadata = metadata
    )

    class ValidationError(
        cause: Throwable? = null,
        rawMessage: String? = cause?.message,
        metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError(
        severity = ErrorSeverity.Error,
        retry = ErrorRetry.Forbidden,
        cause = cause,
        rawMessage = rawMessage,
        metadata = metadata
    )

    /* Ошибки, связанные с лимитами */
    class RateLimitError(
        val retryAfterSeconds: Long? = null,
        cause: Throwable? = null,
        rawMessage: String? = cause?.message,
        metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError(
        severity = ErrorSeverity.Error,
        retry = ErrorRetry.Allowed,
        cause = cause,
        rawMessage = rawMessage,
        metadata = metadata
    )

    /* Ошибки, связанные с пермишенами */
    class PermissionError(
        cause: Throwable? = null,
        rawMessage: String? = cause?.message,
        metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError(
        severity = ErrorSeverity.Error,
        retry = ErrorRetry.Allowed,
        cause = cause,
        rawMessage = rawMessage,
        metadata = metadata
    )

    /* Ошибки доменного слоя */
    class DomainError(
        severity: ErrorSeverity,
        retry: ErrorRetry,
        cause: Throwable? = null,
        rawMessage: String? = cause?.message,
        metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError(
        severity = severity,
        retry = retry,
        cause = cause,
        rawMessage = rawMessage,
        metadata = metadata
    )

    /* Неизвестные ошибки */
    class UnknownError(
        severity: ErrorSeverity,
        retry: ErrorRetry,
        cause: Throwable? = null,
        rawMessage: String? = cause?.message,
        metadata: Map<MetadataKey, String> = emptyMap(),
    ) : AppError(
        severity = severity,
        retry = retry,
        cause = cause,
        rawMessage = rawMessage,
        metadata = metadata
    )
}
