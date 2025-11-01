package ru.izhxx.aichallenge.data.preferences

/**
 * JVM-реализация [ApiKeyStoreFactory]
 */
actual class ApiKeyStoreFactory {
    private var instance: ApiKeyStore? = null

    /**
     * Создает и возвращает экземпляр JvmApiKeyStore
     */
    actual fun create(): ApiKeyStore = JvmApiKeyStore()
}
