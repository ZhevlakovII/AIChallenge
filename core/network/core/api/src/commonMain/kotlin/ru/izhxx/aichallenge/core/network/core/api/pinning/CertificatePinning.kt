package ru.izhxx.aichallenge.core.network.core.api.pinning

/**
 * Абстракция политики пиннинга сертификатов.
 *
 * Реализация зависит от платформы/движка и предоставляется в impl-слое.
 * Core-слой лишь объявляет контракт.
 */
fun interface CertificatePinning {
    /**
     * Должна вернуть true, если сертификат для [host] с отпечатком [certSha256]
     * считается валидным согласно политике пиннинга.
     *
     * Формат отпечатка и способ сопоставления — ответственность реализации.
     */
    fun isPinValid(host: String, certSha256: String): Boolean
}