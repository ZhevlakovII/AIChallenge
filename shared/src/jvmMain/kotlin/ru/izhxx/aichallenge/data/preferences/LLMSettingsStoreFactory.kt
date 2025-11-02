package ru.izhxx.aichallenge.data.preferences

/**
 * JVM-реализация [LLMSettingsStoreFactory]
 */
actual class LLMSettingsStoreFactory {
    private var instance: LLMSettingsStore? = null

    /**
     * Создает и возвращает экземпляр JvmLLMSettingsStore
     */
    actual fun create(): LLMSettingsStore = JvmLLMSettingsStore()
}
