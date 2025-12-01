package ru.izhxx.aichallenge.core.network.impl.di

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendIfNameAbsent
import org.koin.dsl.module
import ru.izhxx.aichallenge.core.network.api.clients.rest.RestClient
import ru.izhxx.aichallenge.core.network.api.clients.sse.SseClient
import ru.izhxx.aichallenge.core.network.api.clients.websocket.WebSocketClient
import ru.izhxx.aichallenge.core.network.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.api.errors.NetworkErrorMapper
import ru.izhxx.aichallenge.core.network.api.factory.HttpClientFactory
import ru.izhxx.aichallenge.core.network.api.factory.HttpEngineFactory
import ru.izhxx.aichallenge.core.network.impl.clients.factory.DefaultHttpClientFactory
import ru.izhxx.aichallenge.core.network.impl.clients.factory.DefaultHttpEngineFactory
import ru.izhxx.aichallenge.core.network.impl.clients.rest.RestClientImpl
import ru.izhxx.aichallenge.core.network.impl.clients.sse.SseClientImpl
import ru.izhxx.aichallenge.core.network.impl.clients.websocket.WebSocketClientImpl
import ru.izhxx.aichallenge.core.network.impl.errors.NetworkErrorMapperImpl
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import io.ktor.client.plugins.logging.Logging as KtorLogging

/**
 * Базовый сетевой модуль.
 * Требует, чтобы в DI уже был предоставлен NetworkConfig (ru.izhxx.aichallenge.core.network.api.config.NetworkConfig).
 *
 * Внимание: теперь не регистрируем глобальный HttpClient-провайдер.
 * Экспортируем Engine/Client фабрики и даём "дефолтный" HttpClient как удобство.
 */
val coreNetworkModule = module {
    // Фабрики
    single<HttpEngineFactory> { DefaultHttpEngineFactory() }
    single<HttpClientFactory> { DefaultHttpClientFactory(get()) }

    // Базовый HttpClient (по умолчанию конфигурируем как раньше, на базе EngineFactory)
    single<HttpClient> {
        val cfg: NetworkConfig = get()
        val engine = get<HttpEngineFactory>().create(cfg)
        engine.config {
            install(ContentNegotiation) { json(cfg.json.toJson()) }
            install(HttpTimeout) {
                connectTimeoutMillis = cfg.timeouts.connectTimeout.inWholeMilliseconds
                requestTimeoutMillis = cfg.timeouts.requestTimeout.inWholeMilliseconds
                socketTimeoutMillis = cfg.timeouts.socketTimeout.inWholeMilliseconds
            }
            install(HttpRequestRetry) {
                retryOnExceptionIf(maxRetries = cfg.retryPolicy.retries) { _, _ -> true }
                exponentialDelay(
                    baseDelayMs = cfg.retryPolicy.baseDelayMs,
                    maxDelayMs = cfg.retryPolicy.maxDelayMs
                )
            }
            install(WebSockets) {
                pingInterval = cfg.ws.pingIntervalMs.toDuration(DurationUnit.MILLISECONDS)
                maxFrameSize = cfg.ws.maxFrameSizeBytes
            }
            install(DefaultRequest) {
                url { takeFrom(cfg.baseUrl.trimEnd('/')) }
                headers.appendIfNameAbsent(HttpHeaders.Accept, ContentType.Application.Json.toString())
                headers.appendIfNameAbsent(HttpHeaders.AcceptEncoding, "gzip")
                cfg.defaultHeaders.forEach { (k, v) -> headers.append(k, v) }
            }
            // Логирование оставляем на усмотрение потребителей через модульную систему.
        }
    }

    // Маппер ошибок
    single<NetworkErrorMapper> { NetworkErrorMapperImpl() }

    // Клиенты
    single<RestClient> { RestClientImpl(get(), get(), get()) }
    single<WebSocketClient> { WebSocketClientImpl(get(), get(), get()) }
    single<SseClient> { SseClientImpl(get(), get(), get()) }
}
