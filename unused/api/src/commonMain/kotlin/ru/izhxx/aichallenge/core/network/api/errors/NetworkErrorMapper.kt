package ru.izhxx.aichallenge.core.network.api.errors

import io.ktor.client.statement.HttpResponse
import ru.izhxx.aichallenge.core.foundation.error.AppError

/**
 * Маппер Throwable/HttpResponse? -> AppError
 * HttpResponse может отсутствовать при сетевых/IO ошибках.
 */
fun interface NetworkErrorMapper {
    fun map(throwable: Throwable, response: HttpResponse?): AppError
}
