package ru.izhxx.aichallenge.core.network.core.api.config

/**
 * Глобальная конфигурация Core Transport Layer.
 *
 * - Не зависит от конкретного транспорта/движка (детали скрыты в impl).
 * - Иммутабельная, с безопасными дефолтами.
 * - Доп. политики включаются через перехватчики/мапперы (см. A2), а пер-запросные
 *   переопределения оформляются через RequestOptions (будет добавлено отдельно).
 */
data class NetworkConfig(
    val serialization: SerializationConfig = SerializationConfig.DefaultStrict,
    val timeouts: TimeoutConfig = TimeoutConfig.Default,
    val logging: LoggingConfig = LoggingConfig.ProductionSafe,
    val security: SecurityConfig = SecurityConfig(),
    val metricsEnabled: Boolean = false
) {
    companion object {
        /**
         * Рекомендуемый набор «production-safe» дефолтов.
         */
        val Default: NetworkConfig = NetworkConfig(
            serialization = SerializationConfig.DefaultStrict,
            timeouts = TimeoutConfig.Default,
            logging = LoggingConfig.ProductionSafe,
            security = SecurityConfig(),
            metricsEnabled = false
        )
    }
}
