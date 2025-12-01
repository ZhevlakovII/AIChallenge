package ru.izhxx.aichallenge.core.network.core.impl.factory

import ru.izhxx.aichallenge.core.network.core.api.CoreHttpClient
import ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.core.api.factory.HttpClientFactory
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ErrorInterceptor
import ru.izhxx.aichallenge.core.network.core.api.interceptor.RequestInterceptor
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ResponseInterceptor
import ru.izhxx.aichallenge.core.network.core.api.mapper.ErrorMapper
import ru.izhxx.aichallenge.core.network.core.impl.CoreHttpClientImpl
import ru.izhxx.aichallenge.core.network.core.impl.engine.defaultEngineFactory
import ru.izhxx.aichallenge.core.network.core.impl.mapper.DefaultErrorMapper

/**
 * Default implementation of HttpClientFactory.
 *
 * Creates CoreHttpClient instances with Ktor backend.
 */
internal class HttpClientFactoryImpl : HttpClientFactory {

    override fun create(
        config: NetworkConfig,
        requestInterceptors: List<RequestInterceptor>,
        responseInterceptors: List<ResponseInterceptor>,
        errorMappers: List<ErrorMapper>,
        errorInterceptors: List<ErrorInterceptor>
    ): CoreHttpClient {
        // Always include DefaultErrorMapper as fallback
        val finalErrorMappers = buildList {
            addAll(errorMappers)
            if (none { it is DefaultErrorMapper }) {
                add(DefaultErrorMapper())
            }
        }

        return CoreHttpClientImpl(
            baseConfig = config,
            globalRequestInterceptors = requestInterceptors,
            globalResponseInterceptors = responseInterceptors,
            globalErrorMappers = finalErrorMappers,
            globalErrorInterceptors = errorInterceptors,
            engineFactory = defaultEngineFactory()
        )
    }
}
