package ru.izhxx.aichallenge.core.network.core.api.config

import ru.izhxx.aichallenge.core.network.core.api.pinning.CertificatePinning

/**
 * Настройки безопасности транспорта для core-слоя.
 *
 * Конкретные механики (pinning/keystore/сертификаты) реализуются на платформенном уровне (impl).
 */
data class SecurityConfig(
    /**
     * Политика пиннинга сертификатов (опционально).
     * При null — pinning отключён.
     */
    val certificatePinning: CertificatePinning? = null,
    /**
     * Список доверенных сертификатов в PEM-формате (опционально).
     * Интерпретация и применение осуществляются на платформенном уровне.
     */
    val trustedCertificatesPem: List<String> = emptyList(),
    /**
     * Глобальные «безопасные» заголовки, добавляемые ко всем запросам
     * (например, User-Agent, X-Platform, X-App-Version). Не должны содержать чувствительных данных.
     */
    val defaultSecurityHeaders: Map<String, String> = emptyMap()
)
