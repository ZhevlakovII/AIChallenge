package ru.izhxx.aichallenge.core.network.core.api

import ru.izhxx.aichallenge.core.foundation.result.AppResult
import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext
import ru.izhxx.aichallenge.core.network.core.api.request.RequestOptions
import ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext

/**
 * Абстракция транспортного HTTP-клиента core-слоя.
 * Не раскрывает деталей реализации (например, Ktor) в публичном API.
 */
interface CoreHttpClient {
    /**
     * Выполняет HTTP-запрос согласно [request] с учётом глобальной конфигурации
     * и пер-запросных переопределений [options].
     *
     * Возвращает [AppResult] с [ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext] без выброса исключений наружу.
     */
    suspend fun execute(
        request: RequestContext,
        options: RequestOptions? = null
    ): AppResult<ResponseContext>
}
