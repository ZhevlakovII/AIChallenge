package ru.izhxx.aichallenge.data.api

import ru.izhxx.aichallenge.data.model.LLMChatRequestDTO
import ru.izhxx.aichallenge.data.model.LLMChatResponseDTO

/**
 * Интерфейс для взаимодействия с API OpenAI
 */
interface OpenAIApi {
    /**
     * Отправляет запрос к API OpenAI с дополнительными настройками API
     * @param request Запрос с настройками API
     * @return Ответ от API OpenAI
     */
    suspend fun sendRequest(request: LLMChatRequestDTO): LLMChatResponseDTO
}
