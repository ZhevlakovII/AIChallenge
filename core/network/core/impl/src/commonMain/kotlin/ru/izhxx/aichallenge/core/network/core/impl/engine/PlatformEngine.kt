package ru.izhxx.aichallenge.core.network.core.impl.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import ru.izhxx.aichallenge.core.network.core.api.config.SecurityConfig

/**
 * Платформенный выбор HttpClientEngineFactory.
 * В commonMain объявляем expect, actual будут в androidMain/jvmMain/iosMain.
 *
 * Для сборки common metadata actual не требуется.
 */
internal expect fun defaultEngineFactory(): HttpClientEngineFactory<*>

/**
 * Creates an HttpClient with platform-specific SSL/TLS configuration.
 *
 * This expect function allows each platform to implement custom certificate
 * handling according to its SSL/TLS capabilities:
 * - Android (OkHttp): Configures TrustManager with custom KeyStore
 * - iOS (Darwin): Uses App Transport Security and platform delegates
 * - JVM (CIO): Configures SSLContext with custom certificates
 *
 * The returned HttpClient already has SSL/TLS configured but still requires
 * plugin installation (timeouts, serialization, etc.) at the call site.
 *
 * @param securityConfig Configuration containing custom certificates and pinning policy
 * @return Configured HttpClient with platform-specific SSL/TLS settings
 */
internal expect fun createSecureHttpClient(securityConfig: SecurityConfig): HttpClient

/**
 * Creates an HttpClient with both security and standard plugin configuration.
 *
 * This is a convenience function that combines SSL/TLS setup with plugin installation.
 * Used as the primary client creation method in CoreHttpClientImpl.
 *
 * @param securityConfig Security configuration (certificates, pinning)
 * @param serializationConfig Serialization settings (JSON, ProtoBuf, etc.)
 * @param timeoutConfig Timeout settings
 * @return Fully configured HttpClient ready for use
 */
internal expect fun createConfiguredHttpClient(
    securityConfig: SecurityConfig,
    serializationConfig: ru.izhxx.aichallenge.core.network.core.api.config.SerializationConfig,
    timeoutConfig: ru.izhxx.aichallenge.core.network.core.api.config.TimeoutConfig
): HttpClient
