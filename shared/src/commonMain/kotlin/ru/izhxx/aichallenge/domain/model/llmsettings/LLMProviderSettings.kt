package ru.izhxx.aichallenge.domain.model.llmsettings

import kotlinx.serialization.Serializable

/**
 * Настройки провайдера LLM API
 * Содержит параметры для подключения к конкретному LLM провайдеру
 */
@Serializable
data class LLMProviderSettings(
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
     * Идентификатор проекта OpenAI
     * Используется в заголовке запроса "OpenAI-Project"
     */
    val openaiProject: String = ""
)
