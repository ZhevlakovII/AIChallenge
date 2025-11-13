package ru.izhxx.aichallenge.data.repository

import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.common.safeApiCall
import ru.izhxx.aichallenge.data.api.OpenAIApi
import ru.izhxx.aichallenge.data.model.ChatMessageDTO
import ru.izhxx.aichallenge.data.model.LLMChatRequestDTO
import ru.izhxx.aichallenge.data.parser.core.ResultParser
import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.config.FormatSystemPrompts
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.response.LLMChoice
import ru.izhxx.aichallenge.domain.model.response.LLMResponse
import ru.izhxx.aichallenge.domain.model.response.LLMUsage
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository
import ru.izhxx.aichallenge.domain.repository.LLMConfigRepository
import ru.izhxx.aichallenge.domain.repository.ProviderSettingsRepository

/**
 * Реализация репозитория для работы с LLM клиентом
 */
class LLMClientRepositoryImpl(
    private val openAIApi: OpenAIApi,
    private val llmConfigRepository: LLMConfigRepository,
    private val providerSettingsRepository: ProviderSettingsRepository,
    private val resultParser: ResultParser,
) : LLMClientRepository {

    // Создаем логгер
    private val logger = Logger.forClass(this)

    /**
     * Получает эффективный системный промпт с учетом настроек
     */
    private suspend fun getEffectiveSystemPrompt(): LLMMessage {
        val promptSettings = llmConfigRepository.getSettings()

        return LLMMessage(
            role = MessageRole.SYSTEM,
            content = """
                ${promptSettings.systemPrompt}
                ${FormatSystemPrompts.getFormatPrompt(promptSettings.responseFormat)}
            """.trimIndent()
        )
    }

    /**
     * Отправляет цепочку диалога с LLM
     * @param messages сообщения пользователя
     * @return результат выполнения запроса с разобранным ответом
     */
    override suspend fun sendMessages(messages: List<LLMMessage>): Result<LLMResponse> {
        if (messages.isEmpty()) {
            return Result.failure(IllegalStateException("Empty messages"))
        }

        return safeApiCall(logger) {
            val lastMessageContent = messages.last().content

            logger.d("Отправка сообщения: \"${lastMessageContent.take(50)}${if (lastMessageContent.length > 50) "..." else ""}\"")

            // Получаем настройки из репозиториев
            val llmConfig = llmConfigRepository.getSettings()
            val providerSettings = providerSettingsRepository.getSettings()

            // Получаем эффективный системный промпт
            val systemMessage = getEffectiveSystemPrompt()

            // Формируем список сообщений: система + предыдущие сообщения + текущее
            val messages = buildList(messages.size + 1) {
                add(systemMessage)
                addAll(messages)
            }

            // Создаем запрос
            val request = LLMChatRequestDTO(
                model = providerSettings.model,
                messages = messages.map { message ->
                    ChatMessageDTO(
                        role = message.role.key,
                        content = message.content
                    )
                },
                temperature = llmConfig.temperature,
                maxTokens = llmConfig.maxTokens,
                topK = llmConfig.topK,
                topP = llmConfig.topP,
                minP = llmConfig.minP,
                topA = llmConfig.topA,
                seed = llmConfig.seed,
                apiKey = providerSettings.apiKey,
                apiUrl = providerSettings.apiUrl,
            )

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
                throw Exception("Empty api response")
            }

            logger.d("Успешно получен ответ: \"${messageContent.take(50)}${if (messageContent.length > 50) "..." else ""}\"")

            // Создаём объект метрик из ответа API
            val metrics = completionResponse.usage?.let { usageDto ->
                LLMUsage(
                    promptTokens = usageDto.promptTokens,
                    completionTokens = usageDto.completionTokens,
                    totalTokens = usageDto.totalTokens,
                    responseTimeMs = responseTime
                )
            }

            // Добавляем метрики к результату
            LLMResponse(
                id = completionResponse.id,
                choices = completionResponse.choices.map { choiceDto ->
                    LLMChoice(
                        index = choiceDto.index,
                        rawMessage = choiceDto.message.let { messageDto ->
                            LLMMessage(
                                role = MessageRole.getRole(messageDto.role),
                                content = messageDto.content
                            )
                        },
                        parsedMessage = resultParser.parse(
                            choiceDto.message.content to llmConfig.responseFormat
                        ).getOrThrow(),
                        finishReason = choiceDto.finishReason
                    )
                },
                format = llmConfig.responseFormat,
                usage = metrics
            )
        }
    }
}
