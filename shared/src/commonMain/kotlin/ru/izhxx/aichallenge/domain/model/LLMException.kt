package ru.izhxx.aichallenge.domain.model

/**
 * Кастомное исключение для ошибок LLM
 * Содержит код ошибки, подробное описание и рекомендации
 */
class LLMException(
    val errorCode: String,
    val detailedMessage: String,
    val userFriendlyMessage: String,
    cause: Throwable? = null
) : Exception(userFriendlyMessage, cause) {
    
    /**
     * Возвращает полное описание ошибки для отладки
     */
    fun getFullErrorInfo(): String {
        return buildString {
            append("Ошибка: $errorCode\n")
            append("Описание: $userFriendlyMessage\n")
            if (detailedMessage.isNotBlank()) {
                append("Детали: $detailedMessage")
            }
        }
    }
    
    /**
     * Возвращает краткое описание ошибки для UI
     */
    fun getShortErrorInfo(): String {
        return "$errorCode: $userFriendlyMessage"
    }
}

/**
 * Фабрика для создания LLMException из различных типов ошибок
 */
object LLMExceptionFactory {
    
    fun createApiKeyNotConfigured(): LLMException {
        return LLMException(
            errorCode = "ERROR_NO_API_KEY",
            userFriendlyMessage = "API ключ не настроен",
            detailedMessage = "API ключ OpenAI не был найден в настройках. Пожалуйста, перейдите в настройки и добавьте ваш API ключ."
        )
    }
    
    fun createApiUrlNotConfigured(): LLMException {
        return LLMException(
            errorCode = "ERROR_NO_API_URL",
            userFriendlyMessage = "URL API не настроен",
            detailedMessage = "URL API LLM не был найден в настройках. Пожалуйста, перейдите в настройки и добавьте URL API."
        )
    }
    
    fun createEmptyResponse(): LLMException {
        return LLMException(
            errorCode = "ERROR_EMPTY_RESPONSE",
            userFriendlyMessage = "Пустой ответ от сервера",
            detailedMessage = "API вернул пустой ответ. Попробуйте отправить запрос еще раз."
        )
    }
    
    fun createNetworkError(statusCode: Int, message: String): LLMException {
        val userMessage = when (statusCode) {
            401 -> "Ошибка аутентификации (API ключ неверный)"
            403 -> "Доступ запрещён"
            429 -> "Лимит запросов превышен"
            500 -> "Ошибка сервера"
            502 -> "Ошибка шлюза"
            503 -> "Сервис недоступен"
            else -> "Ошибка сети (код $statusCode)"
        }
        
        return LLMException(
            errorCode = "ERROR_HTTP_$statusCode",
            userFriendlyMessage = userMessage,
            detailedMessage = "HTTP ошибка $statusCode: $message"
        )
    }
    
    fun createParsingError(message: String): LLMException {
        return LLMException(
            errorCode = "ERROR_PARSE",
            userFriendlyMessage = "Ошибка обработки ответа",
            detailedMessage = "Не удалось распарсить ответ от API: $message"
        )
    }
    
    fun createGenericError(message: String): LLMException {
        return LLMException(
            errorCode = "ERROR_GENERIC",
            userFriendlyMessage = "Неизвестная ошибка",
            detailedMessage = message
        )
    }
    
    fun createConnectionError(message: String): LLMException {
        return LLMException(
            errorCode = "ERROR_CONNECTION",
            userFriendlyMessage = "Ошибка соединения",
            detailedMessage = "Не удалось подключиться к серверу: $message"
        )
    }
}
