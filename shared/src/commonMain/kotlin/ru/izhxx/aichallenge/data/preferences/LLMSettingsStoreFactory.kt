package ru.izhxx.aichallenge.data.preferences

/**
 * Фабрика для создания экземпляров LLMSettingsStore в зависимости от платформы
 */
expect class LLMSettingsStoreFactory() {
    /**
     * Создает и возвращает экземпляр LLMSettingsStore для текущей платформы
     */
    fun create(): LLMSettingsStore
}
