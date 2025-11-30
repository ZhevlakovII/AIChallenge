package ru.izhxx.aichallenge.core.network.core.api.metrics

import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext
import ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext

/**
 * Контракт метрик для Core Transport Layer.
 *
 * Реализации могут публиковать метрики в любые системы.
 * По умолчанию в core-impl будет предоставлена no-op реализация.
 */
interface NetworkMetrics {
    /**
     * Вызывается непосредственно перед отправкой запроса.
     */
    fun onRequestStart(request: RequestContext)

    /**
     * Вызывается после успешного получения ответа.
     */
    fun onRequestSuccess(request: RequestContext, response: ResponseContext)

    /**
     * Вызывается при ошибке обработки запроса (после маппинга в [ru.izhxx.aichallenge.core.foundation.error.AppError]).
     */
    fun onRequestError(request: RequestContext, error: AppError)
}