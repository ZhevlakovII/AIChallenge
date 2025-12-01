package ru.izhxx.aichallenge.core.network.core.api.interceptor

import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext
import ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext

/**
 * Перехватчик ответа core-транспорта.
 *
 * Назначение:
 * - Позволяет проанализировать/модифицировать [ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext] после получения ответа.
 * - Имеет доступ к исходному [RequestContext] (для метрик/логирования/правил).
 *
 * Правила:
 * - Не бросать исключения наружу: ошибки должны обрабатываться транспортом и маппиться в AppError.
 * - Выполняется последовательно в цепочке перехватчиков.
 */
fun interface ResponseInterceptor {
    /**
     * Обработать и (при необходимости) вернуть изменённый [response].
     * Реализация должна быть идемпотентной и не содержать тяжелых блокирующих операций.
     */
    suspend fun intercept(request: RequestContext, response: ResponseContext): ResponseContext
}
