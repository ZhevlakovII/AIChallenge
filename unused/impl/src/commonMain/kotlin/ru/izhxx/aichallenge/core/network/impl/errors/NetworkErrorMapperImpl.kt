package ru.izhxx.aichallenge.core.network.impl.errors

import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CancellationException
import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.foundation.error.ErrorSeverity
import ru.izhxx.aichallenge.core.network.api.errors.NetworkErrorMapper

/**
 * Базовый маппер Throwable/HttpResponse? -> AppError.
 * Можно расширять под особенности движков/платформ.
 */
internal class NetworkErrorMapperImpl : NetworkErrorMapper {

    override fun map(throwable: Throwable, response: HttpResponse?): AppError = when (throwable) {
        is HttpStatusError -> {
            val url = runCatching { response?.request?.url.toString() }.getOrNull() ?: "unknown"
            val method = runCatching { response?.request?.method?.value }.getOrNull() ?: "UNKNOWN"
            val status = throwable.status.value
            val code = "http.$status"
            AppError.HttpError(
                code = code,
                status = status,
                method = method,
                url = url,
                rawMessage = throwable.message ?: "",
                cause = throwable,
                metadata = mapOf(
                    AppError.MetadataKeys.HTTP_STATUS to status.toString(),
                    AppError.MetadataKeys.URL to url,
                    AppError.MetadataKeys.METHOD to method,
                    AppError.MetadataKeys.ORIGIN to "core.network"
                )
            )
        }

        is HttpRequestTimeoutException -> {
            AppError.TimeoutError(
                code = "network.timeout",
                timeoutMillis = null,
                rawMessage = throwable.message ?: "Request timeout",
                cause = throwable,
                metadata = mapOf(
                    AppError.MetadataKeys.ORIGIN to "core.network"
                )
            )
        }

        is CancellationException -> {
            // Обычно отмену не маппим, но чтобы не терять единообразие, вернём UnknownError с кодом cancelled
            AppError.UnknownError(
                code = "cancelled",
                rawMessage = throwable.message ?: "Cancelled",
                cause = throwable,
                metadata = mapOf(AppError.MetadataKeys.ORIGIN to "core.network")
            )
        }

        else -> {
            // Сетевые/IO/прочие ошибки
            AppError.NetworkError(
                code = "network.io",
                rawMessage = throwable.message ?: throwable.toString(),
                cause = throwable,
                severity = ErrorSeverity.Error,
                metadata = buildMap {
                    put(AppError.MetadataKeys.ORIGIN, "core.network")
                    response?.let {
                        put(AppError.MetadataKeys.URL, runCatching { it.request.url.toString() }.getOrNull() ?: "unknown")
                        put(AppError.MetadataKeys.METHOD, runCatching { it.request.method.value }.getOrNull() ?: "UNKNOWN")
                    }
                }
            )
        }
    }
}
