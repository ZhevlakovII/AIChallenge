package ru.izhxx.aichallenge.core.network.core.api.config

import ru.izhxx.aichallenge.core.foundation.logger.LogLevel

/**
 * Настройки логирования core-транспорта.
 *
 * По умолчанию настроены безопасные (production-safe) политики:
 * - минимальная детализация;
 * - запрет логирования тел запросов/ответов;
 * - редактирование чувствительных заголовков и query-параметров.
 */
data class LoggingConfig(
    /** Глобальный уровень логирования сетевого слоя. */
    val level: LogLevel = LogLevel.WARN,
    /** Логировать тела запросов. По умолчанию выключено. */
    val logRequestBody: Boolean = false,
    /** Логировать тела ответов. По умолчанию выключено. */
    val logResponseBody: Boolean = false,
    /** Максимальная длина тела для логирования при включённом body-логировании. */
    val maxBodyChars: Int = 2_048,
    /**
     * Заголовки, значения которых редактируются (маскируются) в логах.
     * Сопоставление выполняется без учёта регистра.
     */
    val redactHeaders: Set<String> = setOf("Authorization", "Cookie", "Set-Cookie"),
    /**
     * Имена query-параметров, значения которых редактируются в логах.
     * Сопоставление выполняется без учёта регистра.
     */
    val redactQueryParams: Set<String> = setOf("token", "api_key", "access_token")
) {
    companion object {
        /** Настройки для production сред: минимально необходимая детализация и безопасные политики. */
        val ProductionSafe = LoggingConfig(
            level = LogLevel.WARN,
            logRequestBody = false,
            logResponseBody = false,
            maxBodyChars = 2_048,
            redactHeaders = setOf("Authorization", "Cookie", "Set-Cookie"),
            redactQueryParams = setOf("token", "api_key", "access_token")
        )

        /** Настройки для отладки: повышенная детализация. Использовать с осторожностью. */
        val DebugVerbose = LoggingConfig(
            level = LogLevel.DEBUG,
            logRequestBody = true,
            logResponseBody = true,
            maxBodyChars = 8_192,
            redactHeaders = setOf("Authorization", "Cookie", "Set-Cookie"),
            redactQueryParams = setOf("token", "api_key", "access_token")
        )
    }
}
