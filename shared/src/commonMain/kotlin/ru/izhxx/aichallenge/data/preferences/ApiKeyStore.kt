package ru.izhxx.aichallenge.data.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для хранения API ключа
 */
interface ApiKeyStore {
    /**
     * Сохраняет API ключ
     * 
     * @param apiKey API ключ для сохранения
     */
    suspend fun saveApiKey(apiKey: String)
    
    /**
     * Возвращает сохраненный API ключ или пустую строку, если ключ не сохранен
     */
    suspend fun getApiKey(): String
    
    /**
     * Поток изменений API ключа
     */
    fun getApiKeyFlow(): Flow<String>
    
    /**
     * Проверяет, был ли API ключ сохранен
     */
    suspend fun hasApiKey(): Boolean
    
    /**
     * Удаляет сохраненный API ключ
     */
    suspend fun clearApiKey()
}
