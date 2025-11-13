package ru.izhxx.aichallenge.common

import ru.izhxx.aichallenge.domain.model.error.DomainException

/**
 * Безопасно вызывает API функцию и преобразует исключения в Result
 * @param logger Логгер для записи ошибок
 * @param apiCall Функция API для вызова
 * @return Result с результатом выполнения или ошибкой
 */
suspend inline fun <T> safeApiCall(
    logger: Logger,
    crossinline apiCall: suspend () -> T
): Result<T> {
    return try {
        Result.success(apiCall())
    } catch (e: Exception) {
        logger.e("Unexpected error: ${e.message}", e)
        Result.failure(
            DomainException(
                detailedMessage = e.message.orEmpty()
            )
        )
    }
}
