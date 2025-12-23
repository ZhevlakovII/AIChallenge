package ru.izhxx.aichallenge.core.safecall

import ru.izhxx.aichallenge.core.errors.AppError
import ru.izhxx.aichallenge.core.errors.ErrorRetry
import ru.izhxx.aichallenge.core.errors.ErrorSeverity
import ru.izhxx.aichallenge.core.errors.MetadataKey
import ru.izhxx.aichallenge.core.result.AppResult
import kotlin.coroutines.cancellation.CancellationException

/**
 * Выполняет блок [block], перехватывая исключения и возвращая [AppResult].
 *
 * Поведение:
 * - Успех — возвращает [AppResult.Success].
 * - Любое исключение — преобразуется через [throwableMapper] и возвращает [AppResult.Failure].
 *
 * Важно: [CancellationException] не перехватывается и пробрасывается, сохраняя семантику отмены.
 *
 * @param T Тип результата.
 * @param throwableMapper Маппер Throwable → [AppError]. По умолчанию — [defaultUnknownError].
 * @param block Операция, потенциально выбрасывающая исключения.
 * @return [AppResult] с успехом или ошибкой.
 *
 * @see AppResult
 * @see AppError
 */
inline fun <T> safeCall(
    crossinline throwableMapper: (Throwable) -> AppError = { t -> defaultUnknownError(t) },
    crossinline block: () -> T
): AppResult<T> {
    return try {
        AppResult.success(block())
    } catch (e: CancellationException) {
        // Не глотаем отмену корутин
        throw e
    } catch (t: Throwable) {
        AppResult.failure(throwableMapper(t))
    }
}

/**
 * Suspend‑вариант выполнения блока [block] с перехватом исключений и возвратом [AppResult].
 *
 * Поведение:
 * - Успех — возвращает [AppResult.Success].
 * - Любое исключение — преобразуется через [throwableMapper] и возвращает [AppResult.Failure].
 *
 * Важно: [CancellationException] не перехватывается и пробрасывается, сохраняя семантику отмены корутин.
 *
 * @param T Тип результата.
 * @param throwableMapper Маппер Throwable → [AppError]. По умолчанию — [defaultUnknownError].
 * @param block Suspend‑операция, потенциально выбрасывающая исключения.
 * @return [AppResult] с успехом или ошибкой.
 *
 * @see AppResult
 * @see AppError
 */
suspend inline fun <T> suspendedSafeCall(
    crossinline throwableMapper: (Throwable) -> AppError = { t -> defaultUnknownError(t) },
    crossinline block: suspend () -> T
): AppResult<T> {
    return try {
        AppResult.success(block())
    } catch (e: CancellationException) {
        // Не глотаем отмену корутин
        throw e
    } catch (t: Throwable) {
        AppResult.failure(throwableMapper(t))
    }
}

/**
 * Suspend‑вариант выполнения блока [block] с перехватом исключений и возвратом [AppResult].
 *
 * Поведение:
 * - Успех — возвращает [AppResult.Success].
 * - Любое исключение — преобразуется через [suspendThrowableMapper] и возвращает [AppResult.Failure].
 *
 * Важно: [CancellationException] не перехватывается и пробрасывается, сохраняя семантику отмены корутин.
 *
 * @param T Тип результата.
 * @param suspendThrowableMapper Suspend-вариант маппера Throwable → [AppError].
 * @param block Suspend‑операция, потенциально выбрасывающая исключения.
 * @return [AppResult] с успехом или ошибкой.
 *
 * @see AppResult
 * @see AppError
 */
suspend inline fun <T> suspendedSafeCall(
    crossinline suspendThrowableMapper: suspend (Throwable) -> AppError,
    crossinline block: suspend () -> T
): AppResult<T> {
    return try {
        AppResult.success(block())
    } catch (e: CancellationException) {
        // Не глотаем отмену корутин
        throw e
    } catch (t: Throwable) {
        AppResult.failure(suspendThrowableMapper(t))
    }
}

/**
 * Формирует [AppError.UnknownError] по умолчанию для немаппированных исключений.
 *
 * Используется как значение по умолчанию для [throwableMapper] в [suspendedSafeCall].
 * Устанавливает rawMessage из [Throwable.message] (или toString), сохраняет [Throwable] в [AppError.UnknownError.cause]
 * и добавляет несколько [MetadataKey] для трассировки источника.
 *
 * @param t Исключение‑источник.
 * @return [AppError.UnknownError] с заполненными полями.
 *
 * @see AppError.UnknownError
 * @see MetadataKey
 */
@PublishedApi
internal fun defaultUnknownError(
    t: Throwable,
    severity: ErrorSeverity = ErrorSeverity.Warning,
    retry: ErrorRetry = ErrorRetry.Unknown
): AppError {
    return AppError.UnknownError(
        severity = severity,
        retry = retry,
        cause = t,
        rawMessage = t.message ?: t.toString(),
        metadata = mapOf(
            MetadataKey("origin") to "core.foundation.safecall",
            MetadataKey("throwable.qualifiedName") to t::class.qualifiedName.orEmpty()
        )
    )
}
