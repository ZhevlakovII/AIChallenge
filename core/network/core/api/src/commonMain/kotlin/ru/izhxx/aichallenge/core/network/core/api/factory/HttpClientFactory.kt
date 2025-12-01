package ru.izhxx.aichallenge.core.network.core.api.factory

import ru.izhxx.aichallenge.core.network.core.api.CoreHttpClient
import ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ErrorInterceptor
import ru.izhxx.aichallenge.core.network.core.api.interceptor.RequestInterceptor
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ResponseInterceptor
import ru.izhxx.aichallenge.core.network.core.api.mapper.ErrorMapper

/**
 * Factory for creating CoreHttpClient instances.
 *
 * Allows creating clients with:
 * - Global configuration
 * - Custom interceptors and mappers
 *
 * Note: Does NOT take NetworkMetrics (moved to plugins).
 */
interface HttpClientFactory {
    /**
     * Creates a new CoreHttpClient with the given configuration.
     *
     * @param config Global network configuration
     * @param requestInterceptors Global request interceptors (executed for all requests)
     * @param responseInterceptors Global response interceptors (executed for all responses)
     * @param errorMappers Global error mappers (fallback to DefaultErrorMapper)
     * @param errorInterceptors Global error observers
     * @return Configured CoreHttpClient instance
     */
    fun create(
        config: NetworkConfig = NetworkConfig.Default,
        requestInterceptors: List<RequestInterceptor> = emptyList(),
        responseInterceptors: List<ResponseInterceptor> = emptyList(),
        errorMappers: List<ErrorMapper> = emptyList(),
        errorInterceptors: List<ErrorInterceptor> = emptyList()
    ): CoreHttpClient
}
