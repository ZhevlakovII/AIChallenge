package ru.izhxx.aichallenge.core.network.core.impl.security

import ru.izhxx.aichallenge.core.network.core.api.pinning.CertificatePinning

/**
 * Simple hash-based certificate pinning implementation.
 *
 * Validates certificates by comparing SHA-256 fingerprints against a pre-configured
 * map of host -> valid certificate hashes.
 *
 * @param pins Map of hostname -> list of valid SHA-256 certificate hashes (hex format, case-insensitive)
 *
 * Example:
 * ```
 * val pinning = CertificatePinningImpl(
 *     pins = mapOf(
 *         "api.example.com" to listOf(
 *             "1234567890abcdef...",
 *             "fedcba0987654321..."
 *         )
 *     )
 * )
 * ```
 */
class CertificatePinningImpl(
    private val pins: Map<String, List<String>>
) : CertificatePinning {
    /**
     * Validates if the certificate for a given host and SHA-256 hash is pinned.
     *
     * @param host The hostname being validated
     * @param certSha256 The SHA-256 fingerprint of the certificate (hex format)
     * @return true if the certificate is valid according to pinning policy (or if no pins are configured for the host)
     */
    override fun isPinValid(host: String, certSha256: String): Boolean {
        val validHashes = pins[host] ?: return true // No pinning configured for this host
        return certSha256.lowercase() in validHashes.map { it.lowercase() }
    }

    companion object {
        /**
         * Creates an empty certificate pinning instance that accepts all certificates.
         *
         * Useful as a default/no-op implementation when pinning is disabled.
         *
         * @return CertificatePinning that always returns true
         */
        fun empty(): CertificatePinning = CertificatePinningImpl(emptyMap())
    }
}
