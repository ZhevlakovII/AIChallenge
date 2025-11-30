package ru.izhxx.aichallenge.core.network.core.api.interceptor

import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext

/**
 * Наблюдатель ошибок core-транспорта.
 *
 * Предназначен для побочных эффектов (логирование, отправка метрик и т.п.).
 * Не должен изменять поток возврата (AppResult) и не должен бросать исключения.
 */
fun interface ErrorInterceptor {
    /**
     * Вызывается при ошибке запроса. Исключения из реализации должны быть проглочены.
     */
    suspend fun onError(request: RequestContext, error: AppError)
}
