package ru.izhxx.aichallenge.domain.model.config

/**
 * Настройки провайдера LLM API
 * Содержит параметры для подключения к конкретному LLM провайдеру
 */
data class ProviderSettings(
    /**
     * API ключ для доступа к сервису LLM
     */
    val apiKey: String,
    
    /**
     * URL API сервиса LLM
     */
    val apiUrl: String,
    
    /**
     * Модель LLM
     */
    val model: String
) {
    companion object {
        /**
         * Создает настройки провайдера по умолчанию (пустые)
         */
        fun default() = ProviderSettings(
            apiKey = "",
            apiUrl = "",
            model = ""
        )
    }
}
