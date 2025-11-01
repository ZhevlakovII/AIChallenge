package ru.izhxx.aichallenge.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.data.preferences.ApiKeyStore
import ru.izhxx.aichallenge.domain.model.openai.ChatCompletionRequest
import ru.izhxx.aichallenge.domain.model.openai.ChatCompletionResponse
import ru.izhxx.aichallenge.domain.model.openai.ChatMessage

class OpenAIClient(private val apiKeyStore: ApiKeyStore) {
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    // Системное сообщение, которое определяет роль агента
    private val systemMessage = ChatMessage(
        role = "system",
        content = "Ты опытный Senior Android Developer. Отвечай на вопросы, связанные с Android-разработкой, с точки зрения эксперта. Давай технически точные ответы, следуя лучшим практикам и актуальным рекомендациям Android-разработки."
    )

    /**
     * Проверяет, настроен ли API ключ
     */
    suspend fun isApiKeyConfigured(): Boolean {
        return apiKeyStore.hasApiKey()
    }
    
    suspend fun sendMessage(userMessage: String): Result<String> {
        try {
            // Получаем API ключ из хранилища
            val apiKey = apiKeyStore.getApiKey()
            if (apiKey.isBlank()) {
                return Result.failure(Exception("API ключ OpenAI не настроен. Пожалуйста, настройте его в настройках."))
            }
            
            val request = ChatCompletionRequest(
                messages = listOf(
                    systemMessage,
                    ChatMessage(role = "user", content = userMessage)
                )
            )

            val response: HttpResponse = httpClient.post("https://api.openai.com/v1/chat/completions") {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $apiKey")
                setBody(request)
            }

            if (response.status.isSuccess()) {
                val completionResponse: ChatCompletionResponse = response.body()
                val messageContent = completionResponse.choices.firstOrNull()?.message?.content ?: 
                    return Result.failure(Exception("Пустой ответ от OpenAI"))
                return Result.success(messageContent)
            } else {
                return Result.failure(Exception("Ошибка API: ${response.status.value}"))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
