package ru.izhxx.aichallenge.core.safecall

import kotlin.coroutines.cancellation.CancellationException

/**
 * Выполняет [block], перехватывая исключения (кроме отмены корутин) и возвращая Result.
 * Для удобства принимает лямбды логирования:
 * - [successLog] вызывается при успешном результате (с самим значением),
 * - [failureLog] вызывается при ошибке (с брошенным исключением).
 *
 * Исключения отмены корутин (CancellationException и производные) не перехватываются и пробрасываются.
 */
inline fun <T> safeCall(
    crossinline successLog: (T) -> Unit = {},
    crossinline failureLog: (Throwable) -> Unit = {},
    block: () -> T
): Result<T> {
    return try {
        val value = block()
        successLog(value)
        Result.success(value)
    } catch (e: CancellationException) {
        // Не глотаем отмену корутин
        throw e
    } catch (t: Throwable) {
        failureLog(t)
        Result.failure(t)
    }
}

/**
 * suspend-вариант [safeCall] для вызовов внутри корутин.
 * Перехватывает все ошибки, кроме отмены, и возвращает Result.
 */
suspend inline fun <T> safeCall(
    crossinline successLog: (T) -> Unit = {},
    crossinline failureLog: (Exception) -> Unit = {},
    crossinline block: suspend () -> T
): Result<T> {
    return try {
        val value = block()
        successLog(value)
        Result.success(value)
    } catch (e: CancellationException) {
        // Не глотаем отмену корутин
        throw e
    } catch (t: Exception) {
        failureLog(t)
        Result.failure(t)
    }
}
