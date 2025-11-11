package ru.izhxx.aichallenge.data.repository

import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.data.api.OpenAIApi
import ru.izhxx.aichallenge.data.parser.ResponseParser
import ru.izhxx.aichallenge.domain.model.FormatSystemPrompts
import ru.izhxx.aichallenge.domain.model.ParsedResponse
import ru.izhxx.aichallenge.domain.model.RequestMetrics
import ru.izhxx.aichallenge.domain.model.openai.ChatMessage
import ru.izhxx.aichallenge.domain.model.openai.LLMChatRequest
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository
import ru.izhxx.aichallenge.domain.repository.LLMPromptSettingsRepository
import ru.izhxx.aichallenge.domain.repository.LLMProviderSettingsRepository

/**
 * Реализация репозитория для работы с LLM клиентом
 */
class LLMClientRepositoryImpl(
    private val openAIApi: OpenAIApi,
    private val providerSettingsRepository: LLMProviderSettingsRepository,
    private val promptSettingsRepository: LLMPromptSettingsRepository
) : LLMClientRepository {

    // Создаем логгер
    private val logger = Logger.forClass(this)

    /**
     * Получает эффективный системный промпт с учетом настроек
     */
    private suspend fun getEffectiveSystemPrompt(): ChatMessage {
        val promptSettings = promptSettingsRepository.getSettings()
        
        val basePrompt = promptSettings.systemPrompt
        
        val formatInstructions = FormatSystemPrompts.getFormatPrompt(promptSettings.responseFormat)
        
        return ChatMessage(
            role = "system",
            content = """
                $basePrompt
                $formatInstructions
            """.trimIndent()
        )
    }
    /**
     * Отправляет сообщение без истории
     * @param messages сообщения пользователя
     * @return результат выполнения запроса с разобранным ответом
     */
    override suspend fun sendMessage(messages: List<ChatMessage>): Result<ParsedResponse> {
        if (messages.isEmpty()) return Result.failure(Exception("Сообщения не переданы"))
        val lastMessageContent = messages.last().content

        logger.d("Отправка сообщения: \"${lastMessageContent.take(50)}${if (lastMessageContent.length > 50) "..." else ""}\"")

        // Получаем настройки из репозиториев
        val providerSettings = providerSettingsRepository.getSettings()
        val promptSettings = promptSettingsRepository.getSettings()

        // Проверяем API ключ
        if (providerSettings.apiKey.isBlank()) {
            logger.w("API ключ не настроен")
            return Result.failure(Exception("API ключ LLM не настроен. Пожалуйста, настройте его в настройках."))
        }

        // Получаем эффективный системный промпт
        val systemMessage = getEffectiveSystemPrompt()

        // Формируем список сообщений: система + предыдущие сообщения + текущее
        val messages = buildList(messages.size + 1) {
            add(systemMessage)
            addAll(messages)
        }

        // Создаем запрос
        val request = LLMChatRequest(
            model = providerSettings.model,
            messages = messages,
            temperature = promptSettings.temperature,
            apiKey = providerSettings.apiKey,
            apiUrl = providerSettings.apiUrl,
            openAIProject = providerSettings.openaiProject
        )

        try {
            // Замеряем время начала запроса
            val startTime = System.currentTimeMillis()
            
            // Отправляем запрос к API
            val completionResponse = openAIApi.sendRequest(request)

            // Вычисляем время выполнения запроса
            val responseTime = System.currentTimeMillis() - startTime

            // Получаем содержимое сообщения
            val messageContent = completionResponse.choices.firstOrNull()?.message?.content

            if (messageContent.isNullOrBlank()) {
                logger.e("Пустой ответ от API")
                return Result.failure(Exception("Пустой ответ от LLM API"))
            }

            logger.d("Успешно получен ответ: \"${messageContent.take(50)}${if (messageContent.length > 50) "..." else ""}\"")

            // Создаём объект метрик из ответа API
            val metrics = completionResponse.usage?.let {
                RequestMetrics(
                    responseTime = responseTime,
                    tokensInput = it.promptTokens,
                    tokensOutput = it.completionTokens,
                    tokensTotal = it.totalTokens
                )
            }

            // Парсим ответ в соответствии с выбранным форматом
            val parsedResult = ResponseParser.parseResponse(messageContent, promptSettings.responseFormat)
            
            // Добавляем метрики к результату
            return parsedResult.map { parsed ->
                parsed.copy(metrics = metrics)
            }
        } catch (e: Exception) {
            // Если произошла ошибка при выполнении запроса или парсинге ответа
            logger.e("Ошибка при выполнении запроса или парсинге ответа", e)
            return Result.failure(e)
        }
    }
}
