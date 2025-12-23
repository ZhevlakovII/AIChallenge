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
sealed class AppError(
    val severity: ErrorSeverity,
    val retry: ErrorRetry,
    val cause: Throwable? = null,
    val rawMessage: String? = cause?.message,
    val metadata: Map<MetadataKey, String> = emptyMap()
) {
    /* Дальше идут конкретные типы ошибок */

    /* Ошибки сетевого уровня */

    /**
     * Сеть недоступна (нет интернет-соединения).
     * Критическая ошибка, требующая восстановления соединения. Повтор разрешен.
     */
    object NetworkUnavailable : AppError(
        severity = ErrorSeverity.Critical,
        retry = ErrorRetry.Allowed
    )

    /**
     * Превышено время ожидания ответа от сервера.
     * Ошибка, возникающая при медленном соединении или перегрузке сервера. Повтор разрешен.
     */
    object NetworkTimeout : AppError(
        severity = ErrorSeverity.Error,
        retry = ErrorRetry.Allowed
    )

    /**
     * Ошибка безопасности сетевого соединения (SSL/TLS).
     * Критическая ошибка, возникающая при проблемах с сертификатами или шифрованием. Повтор запрещен.
     *
     * @property cause Оригинальное исключение безопасности.
     * @property rawMessage Сообщение об ошибке.
     * @property metadata Дополнительная информация (например, URL, сертификат).
     */
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

    /**
     * HTTP ошибка с кодом ответа сервера.
     * Возникает при получении ошибочного HTTP статуса (4xx, 5xx).
     * Повтор разрешен для кодов 429 (Rate Limit) и 5xx (Server Error).
     *
     * @param M Тип распарсенного сообщения об ошибке от сервера.
     * @property code HTTP статус код (например, 404, 500).
     * @property parsedMessage Распарсенное тело ответа (опционально).
     * @property cause Оригинальное исключение.
     * @property rawMessage Сырое сообщение об ошибке.
     * @property metadata Дополнительные данные (автоматически включает код ошибки).
     */
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

    /**
     * Токен доступа истек.
     * Критическая ошибка, требующая обновления токена. Повтор разрешен после обновления.
     */
    object TokenExpired : AppError(
        severity = ErrorSeverity.Critical,
        retry = ErrorRetry.Allowed,
    )

    /**
     * Неверные учетные данные (логин/пароль).
     * Предупреждение, возникающее при попытке входа с некорректными данными. Повтор запрещен.
     */
    object InvalidCredentials : AppError(
        severity = ErrorSeverity.Warning,
        retry = ErrorRetry.Forbidden,
    )

    /**
     * Пользователь не авторизован (требуется вход).
     * Критическая ошибка, требующая аутентификации. Повтор запрещен.
     */
    object Unauthorized : AppError(
        severity = ErrorSeverity.Critical,
        retry = ErrorRetry.Forbidden,
    )

    /**
     * Недостаточно прав доступа для выполнения операции.
     * Критическая ошибка, возникающая при попытке доступа к ресурсу без необходимых разрешений. Повтор запрещен.
     */
    object ForbiddenPermissions : AppError(
        severity = ErrorSeverity.Critical,
        retry = ErrorRetry.Forbidden,
    )

    /* Ошибки, связанные с файловой системой */

    /**
     * Файл не найден по указанному пути.
     * Критическая ошибка, возникающая при попытке доступа к несуществующему файлу. Повтор запрещен.
     */
    object FileNotFound : AppError(
        severity = ErrorSeverity.Critical,
        retry = ErrorRetry.Forbidden,
    )

    /**
     * Ошибка работы с хранилищем (чтение/запись файлов, доступ к директориям).
     * Возникает при проблемах с файловой системой: нет прав, недостаточно места, ошибка ввода-вывода.
     *
     * @property path Путь к файлу или директории, вызвавшему ошибку.
     * @property cause Оригинальное исключение.
     * @property rawMessage Сообщение об ошибке.
     * @property metadata Дополнительная информация.
     */
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

    /**
     * Ошибка сериализации/десериализации данных (JSON, XML и др.).
     * Возникает при невозможности преобразовать данные в объект или наоборот.
     *
     * @property cause Оригинальное исключение сериализации.
     * @property rawMessage Сообщение об ошибке.
     * @property metadata Дополнительная информация (например, имя поля, тип данных).
     */
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

    /**
     * Ошибка валидации данных (некорректный формат, нарушение бизнес-правил).
     * Возникает при проверке данных на соответствие требованиям.
     *
     * @property cause Оригинальное исключение валидации.
     * @property rawMessage Сообщение об ошибке.
     * @property metadata Дополнительная информация (например, поле, правило).
     */
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

    /**
     * Превышен лимит запросов (Rate Limit).
     * Возникает при слишком частых обращениях к API. Повтор разрешен после ожидания.
     *
     * @property retryAfterSeconds Рекомендуемое время ожидания перед повтором (в секундах).
     * @property cause Оригинальное исключение.
     * @property rawMessage Сообщение об ошибке.
     * @property metadata Дополнительная информация.
     */
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

    /**
     * Ошибка прав доступа (например, доступ к камере, микрофону, файлам).
     * Возникает при попытке использовать функционал без предоставленных разрешений. Повтор разрешен после предоставления прав.
     *
     * @property cause Оригинальное исключение.
     * @property rawMessage Сообщение об ошибке.
     * @property metadata Дополнительная информация (например, тип разрешения).
     */
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

    /**
     * Ошибка бизнес-логики (доменного слоя).
     * Используется для ошибок, специфичных для конкретной предметной области приложения.
     *
     * @property severity Уровень критичности ошибки.
     * @property retry Разрешен ли повтор операции.
     * @property cause Оригинальное исключение.
     * @property rawMessage Сообщение об ошибке.
     * @property metadata Дополнительная информация (например, сущность, операция).
     */
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

    /**
     * Неизвестная или необработанная ошибка.
     * Используется как fallback для исключений, которые не удалось классифицировать.
     *
     * @property severity Уровень критичности ошибки.
     * @property retry Разрешен ли повтор операции.
     * @property cause Оригинальное исключение.
     * @property rawMessage Сообщение об ошибке.
     * @property metadata Дополнительная информация.
     */
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
