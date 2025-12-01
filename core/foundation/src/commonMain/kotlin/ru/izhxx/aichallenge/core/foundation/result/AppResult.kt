package ru.izhxx.aichallenge.core.foundation.result

import ru.izhxx.aichallenge.core.foundation.error.AppError

/**
 * Унифицированный результат операции приложения (успех или ошибка).
 *
 * Обёртка над успешным значением [Success] или ошибкой [Failure],
 * удобная для композиции и безопасной обработки без исключений.
 *
 * @param T Тип успешного значения.
 *
 * @see Success
 * @see Failure
 * @see AppError
 */
sealed class AppResult<out T> {

    /**
     * Успешный результат.
     *
     * @param T Тип значения.
     * @property value Успешное вычисленное значение.
     *
     * @see Failure
     */
    data class Success<T>(val value: T) : AppResult<T>()

    /**
     * Неуспешный результат.
     *
     * @property error Доменная ошибка [AppError], описывающая причину сбоя.
     *
     * @see Success
     * @see AppError
     */
    data class Failure(val error: AppError) : AppResult<Nothing>()

    /**
     * Выполняет [block], если результат — [Success].
     *
     * @param block Обработчик успешного значения.
     * @return Исходный [AppResult] для чейнинга.
     */
    inline fun onSuccess(block: (T) -> Unit): AppResult<T> {
        if (this is Success) block(value)
        return this
    }

    /**
     * Выполняет [block], если результат — [Failure].
     *
     * @param block Обработчик ошибки [AppError].
     * @return Исходный [AppResult] для чейнинга.
     */
    inline fun onFailure(block: (AppError) -> Unit): AppResult<T> {
        if (this is Failure) block(error)
        return this
    }

    /**
     * Возвращает значение успеха или null при ошибке.
     *
     * @return [Success.value], если результат успешный; иначе — null.
     */
    fun getSuccessOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    companion object {

        /**
         * Фабрика успешного результата.
         *
         * @param value Значение.
         * @return [AppResult.Success] с переданным значением.
         */
        fun <T> success(value: T): AppResult<T> = Success(value)

        /**
         * Фабрика неуспешного результата.
         *
         * @param error Ошибка [AppError].
         * @return [AppResult.Failure] с переданной ошибкой.
         */
        fun failure(error: AppError): AppResult<Nothing> = Failure(error)
    }
}
