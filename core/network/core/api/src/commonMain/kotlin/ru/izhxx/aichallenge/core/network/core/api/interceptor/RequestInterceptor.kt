package ru.izhxx.aichallenge.core.network.core.api.interceptor

import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext

/**
 * Перехватчик запроса core-транспорта.
 *
 * Назначение:
 * - Позволяет модифицировать [ru.izhxx.aichallenge.core.network.core.api.request.RequestContext] до отправки (добавить/изменить заголовки, query, и т.п.).
 * - Не зависит от конкретной реализации HTTP-клиента.
 *
 * Правила:
 * - Не бросать исключения наружу: ошибки должны обрабатываться на уровне транспорта и маппиться в AppError.
 * - Выполняется последовательно в цепочке перехватчиков.
 */
fun interface RequestInterceptor {
    /**
     * Обработать и (при необходимости) вернуть изменённый [request].
     * Реализация должна быть идемпотентной и не содержать тяжелых блокирующих операций.
     */
    suspend fun intercept(request: RequestContext): RequestContext
}
