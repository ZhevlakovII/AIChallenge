package ru.izhxx.aichallenge.domain.model

import kotlinx.serialization.Serializable

/**
 * Настройки для работы с LLM API
 * Содержит все параметры, которые пользователь может настроить для LLM
 */
@Serializable
data class LLMUserSettings(
    /**
     * API ключ для доступа к сервису LLM
     */
    val apiKey: String = "",
    
    /**
     * URL API сервиса LLM
     */
    val apiUrl: String = "",
    
    /**
     * Модель LLM
     */
    val model: String = "",
    
    /**
     * Температура генерации (влияет на случайность ответов)
     * Диапазон значений: 0.0 - 1.0
     * Более низкие значения делают ответы более детерминированными
     * Более высокие значения делают ответы более случайными и творческими
     */
    val temperature: Double = 0.7,
    
    /**
     * Идентификатор проекта OpenAI
     * Используется в заголовке запроса "OpenAI-Project"
     */
    val openaiProject: String = ""
)
