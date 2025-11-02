package ru.izhxx.aichallenge.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.data.preferences.LLMSettingsStore
import ru.izhxx.aichallenge.domain.model.openai.ChatCompletionRequest
import ru.izhxx.aichallenge.domain.model.openai.ChatCompletionResponse
import ru.izhxx.aichallenge.domain.model.openai.ChatMessage
import io.ktor.client.plugins.logging.Logger as KtorLogger

class OpenAIClient(private val llmSettingsStore: LLMSettingsStore) {
    // Создаем логгер для OpenAIClient
    private val logger = Logger.forClass(this)

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }

        // Устанавливаем плагин логирования
        install(Logging) {
            // Устанавливаем уровень логирования - BODY для отображения тела запроса и ответа
            level = LogLevel.ALL

            // Используем наш Logger для вывода сообщений
            logger = object : KtorLogger {
                override fun log(message: String) {
                    this@OpenAIClient.logger.d("HTTP: $message")
                }
            }
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
        return llmSettingsStore.hasApiKey()
    }

    suspend fun sendMessage(userMessage: String): Result<String> {
        logger.d("Отправка сообщения: \"${userMessage.take(50)}${if (userMessage.length > 50) "..." else ""}\"")

        try {
            // Получаем настройки LLM из хранилища
            val settings = llmSettingsStore.getSettings()
            
            // Проверяем API ключ
            if (settings.apiKey.isBlank()) {
                logger.w("API ключ не настроен")
                return Result.failure(Exception("API ключ LLM не настроен. Пожалуйста, настройте его в настройках."))
            }

            val request = ChatCompletionRequest(
                model = settings.model,
                messages = listOf(
                    systemMessage,
                    ChatMessage(role = "user", content = userMessage)
                ),
                temperature = settings.temperature
            )
            
            // Логируем тело запроса
            logger.d("Request Body: $request")

            // Логируем детали запроса для отладки
            logger.d(
                "Запрос к API: model=${settings.model}, systemPrompt=${systemMessage.content.take(30)}..., userMessage=${
                    userMessage.take(30)
                }..."
            )

            logger.i("Отправка запроса к API")
            val response: HttpResponse = httpClient.post(settings.apiUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${settings.apiKey}")
                header("OpenAI-Project", settings.openaiProject)
                setBody(request)
            }

            // Логируем код ответа и заголовки
            logger.d("Заголовки ответа: ${response.headers}")

            val statusCode = response.status.value
            logger.i("Получен ответ от API, код: $statusCode")

            if (response.status.isSuccess()) {
                val completionResponse: ChatCompletionResponse = response.body()
                val messageContent = completionResponse.choices.firstOrNull()?.message?.content

                if (messageContent.isNullOrBlank()) {
                    logger.e("Пустой ответ от API")
                    return Result.failure(Exception("Пустой ответ от LLM API"))
                }

                logger.d("Успешно получен ответ: \"${messageContent.take(50)}${if (messageContent.length > 50) "..." else ""}\"")
                return Result.success(messageContent)
            } else {
                val errorMessage = "Ошибка API: $statusCode"
                logger.e(errorMessage)
                return Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            logger.e("Ошибка при отправке запроса", e)
            return Result.failure(e)
        }
    }
}
