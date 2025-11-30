package ru.izhxx.aichallenge.core.network.core.impl

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod as KtorHttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.core.foundation.error.AppError
import ru.izhxx.aichallenge.core.foundation.result.AppResult
import ru.izhxx.aichallenge.core.foundation.safecall.suspendedSafeCall
import ru.izhxx.aichallenge.core.network.core.api.CoreHttpClient
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ErrorInterceptor
import ru.izhxx.aichallenge.core.network.core.api.mapper.ErrorMapper
import ru.izhxx.aichallenge.core.network.core.api.request.HttpMethod
import ru.izhxx.aichallenge.core.network.core.api.metrics.NetworkMetrics
import ru.izhxx.aichallenge.core.network.core.api.request.RequestBody
import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext
import ru.izhxx.aichallenge.core.network.core.api.interceptor.RequestInterceptor
import ru.izhxx.aichallenge.core.network.core.api.request.RequestOptions
import ru.izhxx.aichallenge.core.network.core.api.response.ResponseContext
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ResponseInterceptor
import ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.core.impl.engine.defaultEngineFactory
import ru.izhxx.aichallenge.core.network.core.impl.mapper.DefaultErrorMapper
import ru.izhxx.aichallenge.core.network.core.impl.metrics.NoOpNetworkMetrics
import kotlin.system.*

/**
 * Ktor-реализация CoreHttpClient для Core Transport Layer.
 *
 * Детали Ktor спрятаны внутри impl. Публичный API использует core/api контракты.
 *
 * Замечания:
 * - На текущем шаге тело запроса необязательно (см. [RequestBody]); REST-уровень может расширить контракт.
 * - Ошибки не пробрасываются, а маппятся в AppError через ErrorMapper-цепочку.
 */
internal class CoreHttpClientImpl(
    private val baseConfig: NetworkConfig,
    private val globalRequestInterceptors: List<RequestInterceptor> = emptyList(),
    private val globalResponseInterceptors: List<ResponseInterceptor> = emptyList(),
    private val globalErrorMappers: List<ErrorMapper> = listOf(DefaultErrorMapper()),
    private val globalErrorInterceptors: List<ErrorInterceptor> = emptyList(),
    private val metrics: NetworkMetrics = NoOpNetworkMetrics,
    engineFactory: HttpClientEngineFactory<*> = defaultEngineFactory()
) : CoreHttpClient {

    private val httpClient: HttpClient = buildHttpClient(baseConfig, engineFactory)

    override suspend fun execute(
        request: RequestContext,
        options: RequestOptions?
    ): AppResult<ResponseContext> {
        val merged = MergedConfig.from(baseConfig, options)

        // request interceptors
        var reqCtx = request
        for (ri in merged.requestInterceptors) {
            reqCtx = ri.intercept(reqCtx)
        }

        metrics.onRequestStart(reqCtx)

        val startTs = System.currentTimeMillis()

        return suspendedSafeCall(throwableMapper = { t ->
            val error = mapException(merged, reqCtx, t)
            // побочный эффект
            notifyError(merged, reqCtx, error)
            error
        }) {
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

                // тело запроса (если задано через RequestOptions as hint)
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
                    }
                }
            }

            val endTs = System.currentTimeMillis()
            val duration = endTs - startTs

            var respCtx = response.toResponseContext(duration, endTs)

            // response interceptors
            for (ri in merged.responseInterceptors) {
                respCtx = ri.intercept(reqCtx, respCtx)
            }

            if (response.status.value in 200..299) {
                metrics.onRequestSuccess(reqCtx, respCtx)
                AppResult.success(respCtx)
            } else {
                val error = mapOnResponse(merged, reqCtx, respCtx)
                notifyError(merged, reqCtx, error)
                AppResult.failure(error)
            }
        }
    }

    // -------------------- helpers --------------------

    private fun buildHttpClient(
        config: NetworkConfig,
        engineFactory: HttpClientEngineFactory<*>
    ): HttpClient = HttpClient(engineFactory) {
        // Timeouts
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeouts.requestTimeoutMillis
            connectTimeoutMillis = config.timeouts.connectTimeoutMillis
            socketTimeoutMillis = config.timeouts.socketTimeoutMillis
        }

        // Serialization
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = config.serialization.ignoreUnknownKeys
                    isLenient = config.serialization.isLenient
                    encodeDefaults = config.serialization.encodeDefaults
                    allowStructuredMapKeys = config.serialization.allowStructuredMapKeys
                    prettyPrint = config.serialization.prettyPrint
                    explicitNulls = config.serialization.explicitNulls
                    coerceInputValues = config.serialization.coerceInputValues
                }
            )
        }

        // Ktor Logging можно подключить позже; сейчас логируем в пайплайне core при необходимости.
    }

    private fun HttpMethod.toKtor(): KtorHttpMethod = when (this) {
        HttpMethod.GET -> KtorHttpMethod.Get
        HttpMethod.POST -> KtorHttpMethod.Post
        HttpMethod.PUT -> KtorHttpMethod.Put
        HttpMethod.PATCH -> KtorHttpMethod.Patch
        HttpMethod.DELETE -> KtorHttpMethod.Delete
        HttpMethod.HEAD -> KtorHttpMethod.Head
        HttpMethod.OPTIONS -> KtorHttpMethod.Options
        HttpMethod.TRACE -> KtorHttpMethod.Trace
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

    private fun mapOnResponse(
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

    private fun mapException(
        merged: MergedConfig,
        request
