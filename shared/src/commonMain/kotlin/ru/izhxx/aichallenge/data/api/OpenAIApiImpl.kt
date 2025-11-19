package ru.izhxx.aichallenge.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.data.error.ApiError
import ru.izhxx.aichallenge.data.error.RequestError
import ru.izhxx.aichallenge.data.model.LLMChatRequestDTO
import ru.izhxx.aichallenge.data.model.LLMChatResponseDTO
import ru.izhxx.aichallenge.data.model.UsageDTO
import io.ktor.client.plugins.logging.Logger as KtorLogger

/**
 * Реализация OpenAIApi, которая отвечает за HTTP-запросы к API OpenAI
 */
internal class OpenAIApiImpl(
    baseHttpClient: HttpClient,
) : OpenAIApi {
    // Создаем логгер для OpenAIApiImpl
    private val logger = Logger.forClass(this)

    // Создаем HTTP клиент
    private val httpClient = baseHttpClient.config {
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
    override suspend fun sendRequest(request: LLMChatRequestDTO): LLMChatResponseDTO {
        // Логируем детали запроса для отладки
        logger.d("Запрос к API: model=${request.model}, messagesCount=${request.messages.size}")
        logger.i("Отправка запроса к API")

        // Выполняем запрос к API
        val response = try {
            httpClient.post(request.apiUrl) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer ${request.apiKey}")
                setBody(request)
            }
        } catch (e: Exception) {
            logger.e("Ошибка при отправке запроса", e)
            throw RequestError(RequestError.RequestStage.SENDING, e)
        }

        // Логируем код ответа и заголовки
        logger.d("Заголовки ответа: ${response.headers}")
        val statusCode = response.status.value
        logger.i("Получен ответ от API, код: $statusCode")

        if (!response.status.isSuccess()) {
            // Если произошла ошибка, выбрасываем исключение
            val errorBody = try {
                response.body<String>()
            } catch (e: Exception) {
                "Не удалось получить тело ошибки"
            }
            logger.e("Ошибка API: $statusCode, Body: $errorBody")
            throw ApiError(statusCode, errorBody)
        }

        // Если запрос выполнен успешно, получаем тело ответа
        val completionResponse = try {
            response.body<LLMChatResponseDTO>()
        } catch (e: Exception) {
            logger.e("Ошибка при парсинге ответа", e)
            throw RequestError(RequestError.RequestStage.PARSING, e)
        }

        // Проверяем, что ответ валиден:
        // - допускаем content == null, если модель вернула tool_calls (finish_reason == "tool_calls" или есть message.tool_calls)
        val firstChoice = completionResponse.choices.firstOrNull()
        val finishReason = firstChoice?.finishReason
        val toolCalls = firstChoice?.message?.toolCalls
        val hasToolCalls = toolCalls != null && toolCalls.isNotEmpty()
        val messageContent = firstChoice?.message?.content

        if (messageContent.isNullOrBlank() && !hasToolCalls) {
            logger.e("Пустой ответ от API (без tool_calls)")
            throw ApiError(200, "Пустой ответ от сервера")
        }

        if (!messageContent.isNullOrBlank()) {
            logger.d("Успешно получен ответ: \"${messageContent.take(50)}${if (messageContent.length > 50) "..." else ""}\"")
        } else if (hasToolCalls) {
            logger.d("Получен ответ с tool_calls (${toolCalls?.size ?: 0}), finish_reason=$finishReason")
        }
        logger.d("Использовано токенов: вход=${completionResponse.usage?.promptTokens}, выход=${completionResponse.usage?.completionTokens}, всего=${completionResponse.usage?.totalTokens}")

        // Возвращаем ответ, обеспечивая что usage не null
        return completionResponse.copy(
            usage = completionResponse.usage?.copy() ?: UsageDTO(0, 0, 0)
        )
    }
}
