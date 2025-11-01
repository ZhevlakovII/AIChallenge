package ru.izhxx.aichallenge.data.preferences

/**
 * Фабрика для создания экземпляров ApiKeyStore в зависимости от платформы
 */
expect class ApiKeyStoreFactory() {
    /**
     * Создает и возвращает экземпляр ApiKeyStore для текущей платформы
     */
    fun create(): ApiKeyStore
}
