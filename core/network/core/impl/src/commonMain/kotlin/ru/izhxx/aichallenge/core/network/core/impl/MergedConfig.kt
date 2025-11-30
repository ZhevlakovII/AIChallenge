package ru.izhxx.aichallenge.core.network.core.impl

import ru.izhxx.aichallenge.core.network.core.api.ErrorInterceptor
import ru.izhxx.aichallenge.core.network.core.api.ErrorMapper
import ru.izhxx.aichallenge.core.network.core.api.RequestInterceptor
import ru.izhxx.aichallenge.core.network.core.api.ResponseInterceptor
import ru.izhxx.aichallenge.core.network.core.api.RequestOptions
import ru.izhxx.aichallenge.core.network.core.api.config.LoggingConfig
import ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig
import ru.izhxx.aichallenge.core.network.core.api.config.TimeoutConfig

/**
 * Сконсолидированная конфигурация на момент выполнения запроса.
 * Содержит слияние глобальных настроек [NetworkConfig] и локальных [RequestOptions].
 */
internal data class MergedConfig(
    val timeoutsOverride: TimeoutConfig?,
    val loggingOverride: LoggingConfig?,
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
            options: RequestOptions?,
            globalRequestInterceptors: List<RequestInterceptor>,
            globalResponseInterceptors: List<ResponseInterceptor>,
            globalErrorMappers: List<ErrorMapper>,
            globalErrorInterceptors: List<ErrorInterceptor>
        ): MergedConfig {
            val timeoutsOverride = options?.timeoutsOverride
            val loggingOverride = options?.loggingOverride
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
