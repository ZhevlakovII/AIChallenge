package ru.izhxx.aichallenge.core.network.core.impl.config

import ru.izhxx.aichallenge.core.network.core.api.config.LoggingConfig
import ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.core.api.config.TimeoutConfig
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ErrorInterceptor
import ru.izhxx.aichallenge.core.network.core.api.interceptor.RequestInterceptor
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ResponseInterceptor
import ru.izhxx.aichallenge.core.network.core.api.mapper.ErrorMapper
import ru.izhxx.aichallenge.core.network.core.api.request.RequestBody
import ru.izhxx.aichallenge.core.network.core.api.request.RequestContext
import ru.izhxx.aichallenge.core.network.core.api.request.RequestOptions

/**
 * Сконсолидированная конфигурация на момент выполнения запроса.
 * Содержит слияние глобальных настроек [ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig] и локальных [ru.izhxx.aichallenge.core.network.core.api.request.RequestOptions].
 */
internal data class MergedConfig(
    val timeoutsOverride: TimeoutConfig?,
    val loggingOverride: LoggingConfig?,
    val body: RequestBody?,
    val extraHeaders: Map<String, String>,
    val requestInterceptors: List<RequestInterceptor>,
    val responseInterceptors: List<ResponseInterceptor>,
    val errorMappers: List<ErrorMapper>,
    val errorInterceptors: List<ErrorInterceptor>,
    val defaultSecurityHeaders: Map<String, String>
) {
    companion object {
        fun from(
            base: NetworkConfig,
            request: RequestContext,
            options: RequestOptions?,
            globalRequestInterceptors: List<RequestInterceptor>,
            globalResponseInterceptors: List<ResponseInterceptor>,
            globalErrorMappers: List<ErrorMapper>,
            globalErrorInterceptors: List<ErrorInterceptor>
        ): MergedConfig {
            val timeoutsOverride = options?.timeoutsOverride
            val loggingOverride = options?.loggingOverride
            val body = request.body
            val extraHeaders = options?.extraHeaders ?: emptyMap()

            val requestInterceptors = buildList {
                addAll(globalRequestInterceptors)
                if (options != null) addAll(options.requestInterceptors)
            }
            val responseInterceptors = buildList {
                addAll(globalResponseInterceptors)
                if (options != null) addAll(options.responseInterceptors)
            }
            val errorMappers = buildList {
                // локальные мапперы должны иметь приоритет
                if (options != null) addAll(options.errorMappers)
                addAll(globalErrorMappers)
            }
            val errorInterceptors = buildList {
                addAll(globalErrorInterceptors)
                if (options != null) addAll(options.errorInterceptors)
            }

            val defaultSecurityHeaders = base.security.defaultSecurityHeaders

            return MergedConfig(
                timeoutsOverride = timeoutsOverride,
                loggingOverride = loggingOverride,
                body = body,
                extraHeaders = extraHeaders,
                requestInterceptors = requestInterceptors,
                responseInterceptors = responseInterceptors,
                errorMappers = errorMappers,
                errorInterceptors = errorInterceptors,
                defaultSecurityHeaders = defaultSecurityHeaders
            )
        }
    }
}
