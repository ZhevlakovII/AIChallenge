package ru.izhxx.aichallenge.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.domain.model.openai.LLMChatRequest
import ru.izhxx.aichallenge.domain.model.openai.LLMChatResponse
import io.ktor.client.plugins.logging.Logger as KtorLogger

/**
 * Реализация OpenAIApi, которая отвечает за HTTP-запросы к API OpenAI
 */
internal class OpenAIApiImpl(private val json: Json) : OpenAIApi {
    // Создаем логгер для OpenAIApiImpl
    private val logger = Logger.forClass(this)

    // Создаем HTTP клиент
    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }

        // Устанавливаем плагин логирования
        install(Logging) {
            // Устанавливаем уровень логирования - BODY для отображения тела запроса и ответа
            level = LogLevel.ALL

            // Используем наш Logger для вывода сообщений
            logger = object : KtorLogger {
                override fun log(message: String) {
                    this@OpenAIApiImpl.logger.d("HTTP: $message")
                }
            }
        }
    }

    /**
     * Отправляет запрос к API OpenAI с дополнительными настройками API
     * @param request Запрос с настройками API
     * @return Ответ от API OpenAI
     */
    override suspend fun sendRequest(request: LLMChatRequest): LLMChatResponse {
        // Проверяем наличие API ключа
        if (request.apiKey.isBlank()) {
            logger.w("API ключ не настроен")
            throw Exception("API ключ LLM не настроен. Пожалуйста, настройте его в настройках.")
        }
        
        // Проверяем URL API
        if (request.apiUrl.isBlank()) {
            logger.w("URL API не настроен")
            throw Exception("URL API не настроен. Пожалуйста, настройте его в настройках.")
        }

        // Логируем детали запроса для отладки
        logger.d("Request Body: $request")
        logger.d("Запрос к API: model=${request.model}, messagesCount=${request.messages.size}")
        logger.i("Отправка запроса к API")
        
        try {
            
            // Выполняем запрос к API
            val response = httpClient.post(request.apiUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${request.apiKey}")
                if (request.openAIProject.isNotBlank()) {
                    header("OpenAI-Project", request.openAIProject)
                }
                setBody(request)
            }

            // Логируем код ответа и заголовки
            logger.d("Заголовки ответа: ${response.headers}")
            val statusCode = response.status.value
            logger.i("Получен ответ от API, код: $statusCode")

            if (response.status.isSuccess()) {
                // Если запрос выполнен успешно, возвращаем тело ответа
                val completionResponse: LLMChatResponse = response.body()
                
                // Проверяем, что ответ содержит сообщение
                val messageContent = completionResponse.choices.firstOrNull()?.message?.content
                if (messageContent.isNullOrBlank()) {
                    logger.e("Пустой ответ от API")
                    throw Exception("Пустой ответ от LLM API")
                }

                logger.d("Успешно получен ответ: \"${messageContent.take(50)}${if (messageContent.length > 50) "..." else ""}\"")
                return completionResponse
            } else {
                // Если произошла ошибка, выбрасываем исключение
                val errorMessage = "Ошибка API: $statusCode"
                logger.e(errorMessage)
                throw Exception(errorMessage)
            }
        } catch (e: Exception) {
            // Обрабатываем исключения, которые могли возникнуть при выполнении запроса
            logger.e("Ошибка при отправке запроса", e)
            throw Exception("Ошибка при отправке запроса: ${e.message}", e)
        }
    }
}
