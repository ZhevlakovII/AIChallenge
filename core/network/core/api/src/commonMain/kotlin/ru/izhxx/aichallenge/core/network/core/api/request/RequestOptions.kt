package ru.izhxx.aichallenge.core.network.core.api.request

import ru.izhxx.aichallenge.core.network.core.api.interceptor.ErrorInterceptor
import ru.izhxx.aichallenge.core.network.core.api.mapper.ErrorMapper
import ru.izhxx.aichallenge.core.network.core.api.interceptor.ResponseInterceptor
import ru.izhxx.aichallenge.core.network.core.api.config.LoggingConfig
import ru.izhxx.aichallenge.core.network.core.api.config.TimeoutConfig
import ru.izhxx.aichallenge.core.network.core.api.interceptor.RequestInterceptor

/**
 * Пер-запросные переопределения настроек Core Transport Layer.
 *
 * Применяется поверх глобального [ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig] на этапе выполнения запроса.
 * Не содержит зависимостей на конкретную реализацию HTTP-клиента.
 */
data class RequestOptions(
    /** Локальные тайм-ауты (если задано — перекрывают [ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig.timeouts]). */
    val timeoutsOverride: TimeoutConfig? = null,
    /** Локальная политика логирования (если задано — перекрывает [ru.izhxx.aichallenge.core.network.core.api.config.NetworkConfig.logging]). */
    val loggingOverride: LoggingConfig? = null,
    /** Дополнительные заголовки, добавляемые к запросу. */
    val extraHeaders: Map<String, String> = emptyMap(),
    /** Локальные request‑перехватчики (выполняются после глобальных). */
    val requestInterceptors: List<RequestInterceptor> = emptyList(),
    /** Локальные response‑перехватчики (выполняются после глобальных). */
    val responseInterceptors: List<ResponseInterceptor> = emptyList(),
    /** Локальные мапперы ошибок (имеют приоритет над глобальными). */
    val errorMappers: List<ErrorMapper> = emptyList(),
    /** Локальные наблюдатели ошибок. */
    val errorInterceptors: List<ErrorInterceptor> = emptyList()
)
