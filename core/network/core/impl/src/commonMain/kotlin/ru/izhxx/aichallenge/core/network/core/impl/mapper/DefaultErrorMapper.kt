package ru.izhxx.aichallenge.core.network.core.impl.mapper

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.request
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.serialization.SerializationException
import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.network.core.api.mapper.ErrorMapper
import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext
import ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext

/**
 * Базовый маппер ошибок core-транспорта.
 * Общая логика: таймауты, сериализация, HTTP-коды, сетевые проблемы.
 */
internal class DefaultErrorMapper : ErrorMapper {

    override suspend fun mapOnResponse(request: RequestContext, response: ResponseContext): AppError? {
        // Если успешный статус — это не ошибка
        if (response.statusCode in 200..399) return null

        val code = "http.${response.statusCode}"
        // Строим URL только для диагностик
        val url = "${request.baseUrl}${request.path}"

        return AppError.HttpError(
            code = code,
            status = response.statusCode,
            method = request.method.name,
            url = url,
            rawMessage = "HTTP ${response.statusCode} for $url",
            metadata = mapOf(
                AppError.MetadataKeys.HTTP_STATUS to response.statusCode.toString(),
                AppError.MetadataKeys.URL to url,
                AppError.MetadataKeys.METHOD to request.method.name,
                AppError.MetadataKeys.ORIGIN to "core.network.core.impl"
            )
        )
    }

    override suspend fun mapOnException(request: RequestContext, cause: Throwable): AppError? {
        // Таймауты
        if (cause is TimeoutCancellationException) {
            return AppError.TimeoutError(
                code = "network.timeout",
                rawMessage = cause.message ?: cause.toString(),
                cause = cause,
                metadata = mapOf(
                    AppError.MetadataKeys.METHOD to request.method.name,
                    AppError.MetadataKeys.URL to "${request.baseUrl}${request.path}",
                    AppError.MetadataKeys.ORIGIN to "core.network.core.impl"
                )
            )
        }

        // Ошибки сериализации
        if (cause is SerializationException) {
            return AppError.SerializationError(
                code = "serialization.decode_error",
                rawMessage = cause.message ?: cause.toString(),
                cause = cause,
                metadata = mapOf(
                    AppError.MetadataKeys.ORIGIN to "core.network.core.impl"
                )
            )
        }

        // HTTP-ошибки, проброшенные Ktor как исключения
        if (cause is ClientRequestException || cause is ServerResponseException) {
            val resp = (cause as? ClientRequestException)?.response
                ?: (cause as? ServerResponseException)?.response

            val status = resp?.status?.value ?: -1
            val method = resp?.request?.method?.value ?: request.method.name
            val url = resp?.request?.url?.toString() ?: "${request.baseUrl}${request.path}"

            return AppError.HttpError(
                code = "http.$status",
                status = status,
                method = method,
                url = url,
                rawMessage = cause.message ?: "HTTP $status for $url",
                cause = cause,
                metadata = mapOf(
                    AppError.MetadataKeys.HTTP_STATUS to status.toString(),
                    AppError.MetadataKeys.URL to url,
                    AppError.MetadataKeys.METHOD to method,
                    AppError.MetadataKeys.ORIGIN to "core.network.core.impl"
                )
            )
        }

        // Сетевые/прочие ошибки — как NetworkError
        return AppError.NetworkError(
            code = "network.failure",
            rawMessage = cause.message ?: cause.toString(),
            cause = cause,
            metadata = mapOf(
                AppError.MetadataKeys.METHOD to request.method.name,
                AppError.MetadataKeys.URL to "${request.baseUrl}${request.path}",
                AppError.MetadataKeys.ORIGIN to "core.network.core.impl"
            )
        )
    }
}