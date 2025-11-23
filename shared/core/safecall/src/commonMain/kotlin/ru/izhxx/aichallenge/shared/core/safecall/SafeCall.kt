package ru.izhxx.aichallenge.shared.core.safecall

import kotlin.coroutines.cancellation.CancellationException

/**
 * Выполняет [block], перехватывая исключения (кроме отмены корутин) и возвращая Result.
 * Для удобства принимает лямбды логирования:
 * - [onSuccess] вызывается при успешном результате (с самим значением),
 * - [onError] вызывается при ошибке (с брошенным исключением).
 *
 * Исключения отмены корутин (CancellationException и производные) не перехватываются и пробрасываются.
 */
inline fun <T> safeCall(
    crossinline onSuccess: (T) -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
    block: () -> T
): Result<T> {
    return try {
        val value = block()
        onSuccess(value)
        Result.success(value)
    } catch (e: CancellationException) {
        // Не глотаем отмену корутин
        throw e
    } catch (t: Throwable) {
        onError(t)
        Result.failure(t)
    }
}

/**
 * suspend-вариант [safeCall] для вызовов внутри корутин.
 * Перехватывает все ошибки, кроме отмены, и возвращает Result.
 */
suspend inline fun <T> safeCallSuspend(
    crossinline onSuccess: (T) -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
    crossinline block: suspend () -> T
): Result<T> {
    return try {
        val value = block()
        onSuccess(value)
        Result.success(value)
    } catch (e: CancellationException) {
        // Не глотаем отмену корутин
        throw e
    } catch (t: Throwable) {
        onError(t)
        Result.failure(t)
    }
}
