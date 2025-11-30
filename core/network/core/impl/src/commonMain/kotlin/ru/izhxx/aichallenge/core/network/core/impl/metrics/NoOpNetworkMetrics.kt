package ru.izhxx.aichallenge.core.network.core.impl.metrics

import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.network.core.api.metrics.NetworkMetrics
import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext
import ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext

/**
 * Пустая реализация метрик (по умолчанию).
 * Ничего не делает, оставлена как безопасная дефолтная реализация.
 */
internal object NoOpNetworkMetrics : NetworkMetrics {
    override fun onRequestStart(request: RequestContext) = Unit
    override fun onRequestSuccess(request: RequestContext, response: ResponseContext) = Unit
    override fun onRequestError(request: RequestContext, error: AppError) = Unit
}