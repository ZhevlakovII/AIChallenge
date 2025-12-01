package ru.izhxx.aichallenge.core.network.core.api.mapper

import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext
import ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext

/**
 * Маппер ошибок core-транспорта в [ru.izhxx.aichallenge.core.foundation.error.AppError].
 *
 * Задачи:
 * - Преобразовать HTTP-ответ/контекст в прикладочную ошибку (например, разобрать error-body).
 * - Преобразовать исключения транспорта/сериализации в [ru.izhxx.aichallenge.core.foundation.error.AppError].
 *
 * Примечания:
 * - Возвращайте null, если маппер не распознал ситуацию — цепочка перейдёт к следующему мапперу.
 * - Не бросайте исключения наружу — пайплайн транспорта обеспечит безопасный вызов.
 */
interface ErrorMapper {
    /**
     * Маппинг на основе полученного [response] (например, по статусу/заголовкам/части тела).
     * Возвращает [ru.izhxx.aichallenge.core.foundation.error.AppError] или null, если данный маппер ситуацию не обрабатывает.
     */
    suspend fun mapOnResponse(request: RequestContext, response: ResponseContext): AppError?

    /**
     * Маппинг на основе возникшего исключения [cause] (сетевые/TLS/таймаут/сериализация и т.п.).
     * Возвращает [AppError] или null, если данный маппер ситуацию не обрабатывает.
     */
    suspend fun mapOnException(request: RequestContext, cause: Throwable): AppError?
}
