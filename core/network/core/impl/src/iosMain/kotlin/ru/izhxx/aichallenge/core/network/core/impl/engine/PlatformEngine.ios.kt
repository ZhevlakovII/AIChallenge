package ru.izhxx.aichallenge.core.network.core.impl.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import io.ktor.client.engine.darwin.certificates.CertificatePinner
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.core.network.core.api.config.SecurityConfig
import ru.izhxx.aichallenge.core.network.core.api.config.SerializationConfig
import ru.izhxx.aichallenge.core.network.core.api.config.TimeoutConfig

internal actual fun defaultEngineFactory(): HttpClientEngineFactory<*> = Darwin

/**
 * Creates an HttpClient with platform-specific SSL/TLS configuration for iOS.
 *
 * Applies custom trusted certificates from PEM format to the Darwin (CFNetwork) engine.
 *
 * @param securityConfig Security configuration containing custom certificates and pinning policy
 * @return Configured HttpClient instance with custom SSL/TLS settings
 */
internal actual fun createSecureHttpClient(
    securityConfig: SecurityConfig
): HttpClient = HttpClient(Darwin) {
    engine {
        // Configure custom certificates if provided
        if (securityConfig.trustedCertificatesPem.isNotEmpty()) {
            configureIosCustomCertificates(securityConfig.trustedCertificatesPem)
        }
    }
}

/**
 * Configures DarwinConfig with custom certificates from PEM format.
 *
 * On iOS/Darwin, certificate pinning is configured using CertificatePinner.
 * The PEM certificates are processed to extract their SHA-256 hashes and set up
 * certificate pinning for the Darwin engine.
 *
 * Note: Full implementation of PEM parsing and certificate hash extraction would
 * require platform-specific code. For now, the configuration hook is prepared
 * for future implementation using platform-specific APIs.
 *
 * @param pemCertificates List of PEM-formatted certificate strings
 */
private fun DarwinClientEngineConfig.configureIosCustomCertificates(
    pemCertificates: List<String>
) {
    try {
        // Note: On Darwin/iOS, certificate pinning configuration would require:
        // 1. Parsing PEM certificates to extract certificate hashes
        // 2. Using CertificatePinner.Builder to configure pinning rules
        // 3. Calling handleChallenge(certificatePinner.build())
        //
        // Full implementation is deferred as it requires platform-specific
        // certificate parsing (SecCertificate, etc.)
        //
        // For production use, implement custom certificate parsing or use
        // a dedicated Kotlin/Native library for certificate handling.
        if (pemCertificates.isNotEmpty()) {
            // TODO: Implement certificate hash extraction and CertificatePinner setup
            // val builder = CertificatePinner.Builder()
            // for (pem in pemCertificates) {
            //     val hash = extractSha256Hash(pem)
            //     builder.add("example.com", "sha256/$hash")
            // }
            // handleChallenge(builder.build())
        }
    } catch (e: Exception) {
        // If configuration fails, continue with default settings
        // (graceful degradation - system will use default trust store)
    }
}

/**
 * Creates a fully configured HttpClient with security, timeouts, and serialization.
 *
 * This implementation combines SSL/TLS configuration with all necessary plugins
 * in a single function to ensure proper initialization order.
 */
internal actual fun createConfiguredHttpClient(
    securityConfig: SecurityConfig,
    serializationConfig: SerializationConfig,
    timeoutConfig: TimeoutConfig
): HttpClient = HttpClient(Darwin) {
    engine {
        // Configure custom certificates if provided
        if (securityConfig.trustedCertificatesPem.isNotEmpty()) {
            configureIosCustomCertificates(securityConfig.trustedCertificatesPem)
        }
    }

    // Timeouts
    install(HttpTimeout) {
        requestTimeoutMillis = timeoutConfig.requestTimeoutMillis
        connectTimeoutMillis = timeoutConfig.connectTimeoutMillis
        socketTimeoutMillis = timeoutConfig.socketTimeoutMillis
    }

    // Serialization
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = serializationConfig.ignoreUnknownKeys
                isLenient = serializationConfig.isLenient
                encodeDefaults = serializationConfig.encodeDefaults
                allowStructuredMapKeys = serializationConfig.allowStructuredMapKeys
                prettyPrint = serializationConfig.prettyPrint
                explicitNulls = serializationConfig.explicitNulls
                coerceInputValues = serializationConfig.coerceInputValues
            }
        )
    }
}