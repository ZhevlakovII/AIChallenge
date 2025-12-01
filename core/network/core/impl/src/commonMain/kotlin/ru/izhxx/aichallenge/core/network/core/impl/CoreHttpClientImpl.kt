package ru.izhxx.aichallenge.core.network.core.impl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod as KtorHttpMethod
import io.ktor.http.contentType
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.flattenEntries
import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.foundation.result.AppResult
import ru.izhxx.aichallenge.core.foundation.safecall.suspendedSafeCall
import ru.izhxx.aichallenge.core.network.core.api.CoreHttpClient
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ErrorInterceptor
import ru.izhxx.aichallenge.core.network.core.api.mapper.ErrorMapper
import ru.izhxx.aichallenge.core.network.core.api.request.HttpMethod
import ru.izhxx.aichallenge.core.network.core.api.request.RequestBody
import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext
import ru.izhxx.aichallenge.core.network.core.api.interceptor.RequestInterceptor
import ru.izhxx.aichallenge.core.network.core.api.request.RequestOptions
import ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ResponseInterceptor
import ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.core.impl.config.MergedConfig
import ru.izhxx.aichallenge.core.network.core.impl.engine.createConfiguredHttpClient
import ru.izhxx.aichallenge.core.network.core.impl.engine.defaultEngineFactory
import ru.izhxx.aichallenge.core.network.core.impl.mapper.DefaultErrorMapper
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Ktor-реализация CoreHttpClient для Core Transport Layer.
 *
 * Детали Ktor спрятаны внутри impl. Публичный API использует core/api контракты.
 *
 * Замечания:
 * - На текущем шаге тело запроса необязательно (см. [RequestBody]); REST-уровень может расширить контракт.
 * - Ошибки не пробрасываются, а маппятся в AppError через ErrorMapper-цепочку.
 */
@OptIn(ExperimentalTime::class)
internal class CoreHttpClientImpl(
    private val baseConfig: NetworkConfig,
    private val globalRequestInterceptors: List<RequestInterceptor> = emptyList(),
    private val globalResponseInterceptors: List<ResponseInterceptor> = emptyList(),
    private val globalErrorMappers: List<ErrorMapper> = listOf(DefaultErrorMapper()),
    private val globalErrorInterceptors: List<ErrorInterceptor> = emptyList(),
    engineFactory: HttpClientEngineFactory<*> = defaultEngineFactory()
) : CoreHttpClient {

    private val httpClient: HttpClient = buildHttpClient(baseConfig, engineFactory, baseConfig.security)

    override suspend fun execute(
        request: RequestContext,
        options: RequestOptions?
    ): AppResult<ResponseContext> {
        val merged = MergedConfig.from(
            baseConfig,
            request,
            options,
            globalRequestInterceptors,
            globalResponseInterceptors,
            globalErrorMappers,
            globalErrorInterceptors
        )

        // request interceptors
        var reqCtx = request
        for (ri in merged.requestInterceptors) {
            reqCtx = ri.intercept(reqCtx)
        }

        val startTs = Clock.System.now().toEpochMilliseconds()

        return suspendedSafeCall(
            throwableMapper = { t ->
                val error = mapException(merged, reqCtx, t)
                // побочный эффект
                notifyError(merged, reqCtx, error)
                error
            }
        ) {
            val response = httpClient.request {
                method = reqCtx.method.toKtor()
                url {
                    // формируем URL: baseUrl + path
                    val base = reqCtx.baseUrl.trimEnd('/')
                    val p = reqCtx.path.trimStart('/')
                    takeFrom("$base/$p")
                    // query params
                    reqCtx.query.forEach { (k, v) ->
                        if (v == null) parameters.append(k, "")
                        else parameters.append(k, v)
                    }
                }
                // security headers (глобальные) + явные из RequestContext + per-request extras
                merged.defaultSecurityHeaders.forEach { (k, v) -> headers.append(k, v) }
                reqCtx.headers.forEach { (k, v) -> headers.append(k, v) }
                merged.extraHeaders.forEach { (k, v) -> headers.append(k, v) }

                // тело запроса (prefer RequestContext.body, fallback to RequestOptions.body)
                merged.body?.let { b ->
                    when (b) {
                        is RequestBody.Text -> {
                            b.contentType?.let { ct -> contentType(ContentType.parse(ct)) }
                            setBody(b.text)
                        }

                        is RequestBody.Bytes -> {
                            b.contentType?.let { ct -> contentType(ContentType.parse(ct)) }
                            setBody(b.bytes)
                        }

                        is RequestBody.Json -> {
                            // JSON-строка, контент-тайп зададим по умолчанию, если не пришёл
                            contentType(ContentType.parse(b.contentType))
                            setBody(b.jsonString)
                        }

                        is RequestBody.Multipart -> {
                            // Multipart form data with files and fields
                            // Set the content-type header with boundary
                            contentType(ContentType.parse(b.contentType))

                            // Encode multipart body
                            val boundary = b.boundary ?: "----KotlinMultipartBoundary"
                            val body = buildMultipartBody(b.parts, boundary)
                            setBody(body)
                        }

                        is RequestBody.Stream -> {
                            // Stream body - get bytes from provider
                            val streamBytes = b.provider()
                            b.contentType?.let { ct -> contentType(ContentType.parse(ct)) }
                            if (b.contentLength != null) {
                                headers.append("Content-Length", b.contentLength.toString())
                            }
                            setBody(streamBytes)
                        }
                    }
                }
            }

            val endTs = Clock.System.now().toEpochMilliseconds()
            val duration = endTs - startTs

            var respCtx = response.toResponseContext(duration, endTs)

            // response interceptors
            for (ri in merged.responseInterceptors) {
                respCtx = ri.intercept(reqCtx, respCtx)
            }

            if (response.status.value in 200..299) {
                respCtx
            } else {
                throw ServerResponseException(response, "")
            }
        }
    }

    // -------------------- helpers --------------------

    private fun buildMultipartBody(
        parts: List<RequestBody.Multipart.Part>,
        boundary: String
    ): ByteArray {
        // Build multipart body with proper byte handling
        val result = mutableListOf<ByteArray>()
        val crlf = "\r\n".encodeToByteArray()

        for (part in parts) {
            // Add boundary
            result.add("--$boundary".encodeToByteArray())
            result.add(crlf)

            when (part) {
                is RequestBody.Multipart.Part.FormField -> {
                    // Add headers
                    result.add("Content-Disposition: form-data; name=\"${part.name}\"".encodeToByteArray())
                    result.add(crlf)
                    result.add(crlf)

                    // Add field value
                    result.add(part.value.encodeToByteArray())
                    result.add(crlf)
                }

                is RequestBody.Multipart.Part.FileData -> {
                    // Add headers
                    result.add("Content-Disposition: form-data; name=\"${part.name}\"; filename=\"${part.filename}\"".encodeToByteArray())
                    result.add(crlf)
                    result.add("Content-Type: ${part.contentType}".encodeToByteArray())
                    result.add(crlf)
                    result.add(crlf)

                    // Add file data
                    result.add(part.bytes)
                    result.add(crlf)
                }
            }
        }

        // Add final boundary
        result.add("--$boundary--".encodeToByteArray())
        result.add(crlf)

        // Concatenate all byte arrays
        val totalSize = result.sumOf { it.size }
        val body = ByteArray(totalSize)
        var offset = 0
        for (bytes in result) {
            bytes.copyInto(body, offset)
            offset += bytes.size
        }

        return body
    }

    private fun buildHttpClient(
        config: NetworkConfig,
        engineFactory: HttpClientEngineFactory<*>,
        securityConfig: ru.izhxx.aichallenge.core.network.core.api.config.SecurityConfig
    ): HttpClient {
        // Use platform-specific factory to create fully configured HttpClient
        // with security, timeouts, and serialization all set up correctly
        return createConfiguredHttpClient(
            securityConfig = securityConfig,
            serializationConfig = config.serialization,
            timeoutConfig = config.timeouts
        )
    }

    private fun HttpMethod.toKtor(): KtorHttpMethod = when (this) {
        HttpMethod.GET -> KtorHttpMethod.Get
        HttpMethod.POST -> KtorHttpMethod.Post
        HttpMethod.PUT -> KtorHttpMethod.Put
        HttpMethod.PATCH -> KtorHttpMethod.Patch
        HttpMethod.DELETE -> KtorHttpMethod.Delete
        HttpMethod.HEAD -> KtorHttpMethod.Head
        HttpMethod.OPTIONS -> KtorHttpMethod.Options
    }

    private suspend fun HttpResponse.toResponseContext(
        durationMillis: Long,
        endTimestampMillis: Long
    ): ResponseContext {
        val headersMap = headers.flattenEntries().associate { (k, v) -> k to v }
        // Пытаемся получить тело как bytes, в противном случае оставляем null
        val bodyBytes = runCatching { this.body<ByteArray>() }.getOrNull()
        return ResponseContext(
            statusCode = status.value,
            headers = headersMap,
            bodyBytes = bodyBytes,
            durationMillis = durationMillis,
            endTimestampMillis = endTimestampMillis
        )
    }

    private suspend fun mapOnResponse(
        merged: MergedConfig,
        request: RequestContext,
        response: ResponseContext
    ): AppError {
        for (m in merged.errorMappers) {
            val mapped = runCatching { m.mapOnResponse(request, response) }.getOrNull()
            if (mapped != null) return mapped
        }
        // fallback — Unknown
        return AppError.UnknownError(
            rawMessage = "Unhandled HTTP ${response.statusCode} for ${request.baseUrl}${request.path}",
            metadata = mapOf(
                AppError.MetadataKeys.HTTP_STATUS to response.statusCode.toString(),
                AppError.MetadataKeys.URL to "${request.baseUrl}${request.path}",
                AppError.MetadataKeys.METHOD to request.method.name,
                AppError.MetadataKeys.ORIGIN to "core.network.core.impl"
            )
        )
    }

    private suspend fun mapException(
        merged: MergedConfig,
        request: RequestContext,
        cause: Throwable
    ): AppError {
        for (m in merged.errorMappers) {
            val mapped = runCatching { m.mapOnException(request, cause) }.getOrNull()
            if (mapped != null) return mapped
        }
        // fallback — NetworkError
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

    private suspend fun notifyError(
        merged: MergedConfig,
        request: RequestContext,
        error: AppError
    ) {
        for (ei in merged.errorInterceptors) {
            ei.onError(request, error)
        }
    }
}
