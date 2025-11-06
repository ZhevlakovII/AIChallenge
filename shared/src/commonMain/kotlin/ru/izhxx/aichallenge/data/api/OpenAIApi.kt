package ru.izhxx.aichallenge.data.api

import ru.izhxx.aichallenge.domain.model.openai.LLMChatRequest
import ru.izhxx.aichallenge.domain.model.openai.LLMChatResponse

interface OpenAIApi {
    /**
     * Отправляет запрос к API OpenAI с дополнительными настройками API
     * @param request Запрос с настройками API
     * @return Ответ от API OpenAI
     */
    suspend fun sendRequest(request: LLMChatRequest): LLMChatResponse
}
