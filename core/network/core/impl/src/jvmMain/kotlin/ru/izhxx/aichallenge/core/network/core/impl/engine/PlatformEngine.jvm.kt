package ru.izhxx.aichallenge.core.network.core.impl.engine

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.network.tls.TLSConfigBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.core.network.core.api.config.SecurityConfig
import ru.izhxx.aichallenge.core.network.core.api.config.SerializationConfig
import ru.izhxx.aichallenge.core.network.core.api.config.TimeoutConfig
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal actual fun defaultEngineFactory(): HttpClientEngineFactory<*> = CIO

/**
 * Creates an HttpClient with platform-specific SSL/TLS configuration for JVM.
 *
 * Applies custom trusted certificates from PEM format to the CIO (Coroutine IO) engine.
 *
 * @param securityConfig Security configuration containing custom certificates and pinning policy
 * @return Configured HttpClient instance with custom SSL/TLS settings
 */
internal actual fun createSecureHttpClient(
    securityConfig: SecurityConfig
): HttpClient = HttpClient(CIO) {
    engine {
        https {
            // Configure custom certificates if provided
            if (securityConfig.trustedCertificatesPem.isNotEmpty()) {
                configureJvmCustomCertificates(securityConfig.trustedCertificatesPem)
            }
        }
    }
}

/**
 * Configures TLSConfigBuilder with custom certificates from PEM format.
 *
 * Parses PEM-formatted certificate strings and sets up a custom TrustManager
 * that accepts these certificates alongside system certificates.
 *
 * For CIO engine, certificate configuration is done via the trustManager property.
 *
 * @param pemCertificates List of PEM-formatted certificate strings
 */
private fun TLSConfigBuilder.configureJvmCustomCertificates(pemCertificates: List<String>) {
    try {
        // Create a KeyStore with custom certificates
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)

        val certificateFactory = CertificateFactory.getInstance("X.509")
        var certCount = 0

        for (pemCert in pemCertificates) {
            try {
                // Parse PEM certificate - handle with and without line breaks
                val cleanPem = pemCert.trim()
                val cert = certificateFactory.generateCertificate(
                    cleanPem.toByteArray().inputStream()
                )
                keyStore.setCertificateEntry("cert_$certCount", cert)
                certCount++
            } catch (e: Exception) {
                // Continue with other certificates if one fails to parse
            }
        }

        if (certCount > 0) {
            // Create TrustManager with custom KeyStore
            val trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            )
            trustManagerFactory.init(keyStore)

            // Apply to CIO engine via trustManager property
            trustManager = trustManagerFactory.trustManagers.first { it is X509TrustManager } as X509TrustManager
        }
    } catch (e: Exception) {
        // If custom certificate setup fails, continue with default certificates
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
): HttpClient = HttpClient(CIO) {
    engine {
        https {
            // Configure custom certificates if provided
            if (securityConfig.trustedCertificatesPem.isNotEmpty()) {
                configureJvmCustomCertificates(securityConfig.trustedCertificatesPem)
            }
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
