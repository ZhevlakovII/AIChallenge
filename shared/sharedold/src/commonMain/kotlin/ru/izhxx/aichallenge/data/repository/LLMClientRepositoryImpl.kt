package ru.izhxx.aichallenge.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.common.safeApiCall
import ru.izhxx.aichallenge.data.api.OpenAIApi
import ru.izhxx.aichallenge.data.model.ChatMessageDTO
import ru.izhxx.aichallenge.data.model.LLMChatRequestDTO
import ru.izhxx.aichallenge.data.model.LLMChatResponseDTO
import ru.izhxx.aichallenge.data.model.LlmToolSchemaDTO
import ru.izhxx.aichallenge.data.model.ToolCallDTO
import ru.izhxx.aichallenge.data.parser.core.ResultParser
import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.config.FormatSystemPrompts
import ru.izhxx.aichallenge.domain.model.config.LLMConfig
import ru.izhxx.aichallenge.domain.model.github.Repo
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.response.LLMChoice
import ru.izhxx.aichallenge.domain.model.response.LLMResponse
import ru.izhxx.aichallenge.domain.model.response.LLMUsage
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository
import ru.izhxx.aichallenge.domain.repository.LLMConfigRepository
import ru.izhxx.aichallenge.domain.repository.McpConfigRepository
import ru.izhxx.aichallenge.domain.repository.ProviderSettingsRepository
import ru.izhxx.aichallenge.mcp.data.McpToLlmToolsMapper
import ru.izhxx.aichallenge.mcp.domain.repository.McpRepository
import ru.izhxx.aichallenge.mcp.domain.usecase.GetMcpServersUseCase
import ru.izhxx.aichallenge.mcp.orchestrator.McpRouter
import ru.izhxx.aichallenge.instruments.user.profile.repository.api.UserProfileRepository

/**
 * Реализация репозитория для работы с LLM клиентом.
 *
 * Добавляет поддержку function calling (tools) с использованием MCP:
 * - Подготовка списка инструментов (tools) из MCP (несколько серверов через оркестратор).
 * - Обработка tool_calls от модели с маршрутизацией на подходящий MCP и возвратом tool_result.
 */
class LLMClientRepositoryImpl(
    private val openAIApi: OpenAIApi,
    private val llmConfigRepository: LLMConfigRepository,
    private val providerSettingsRepository: ProviderSettingsRepository,
    private val resultParser: ResultParser,
    private val mcpRepository: McpRepository,
    private val mcpConfigRepository: McpConfigRepository,
    private val toolsMapper: McpToLlmToolsMapper,
    private val json: Json,
    private val mcpRouter: McpRouter,
    private val getMcpServers: GetMcpServersUseCase,
    private val userProfileRepository: UserProfileRepository
) : LLMClientRepository {

    // Создаем логгер
    private val logger = Logger.forClass(this)

    private companion object {
        /**
         * Максимальное количество раундов обработки tool_calls.
         */
        private const val MAX_TOOL_ROUNDS = 10
    }

    /**
     * Получает эффективный системный промпт с учетом настроек, персонализации и суммаризации
     */
    private suspend fun getEffectiveSystemPrompt(summary: String?): LLMMessage {
        val promptSettings = llmConfigRepository.getSettings()
        val userProfile = userProfileRepository.getProfile()

        val basePrompt = """
            ${promptSettings.systemPrompt}
            ${FormatSystemPrompts.getFormatPrompt(promptSettings.responseFormat)}
        """.trimIndent()

        // Добавляем персонализацию ПЕРЕД базовым промптом (если включена)
        val personalizationText = userProfile.toPromptText()
        val promptWithPersonalization = if (personalizationText != null) {
            """
            $personalizationText

            $basePrompt
            """.trimIndent()
        } else {
            basePrompt
        }

        // Если есть суммаризация, добавляем её в конец
        val finalPrompt = if (summary != null) {
            """
            $promptWithPersonalization

            $summary
            """.trimIndent()
        } else {
            promptWithPersonalization
        }

        return LLMMessage(
            role = MessageRole.SYSTEM,
            content = finalPrompt
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
        return sendMessagesWithSummary(messages, null)
    }

    /**
     * Отправляет цепочку диалога с LLM с учетом суммаризации и (опционально) function calling через MCP.
     * @param messages сообщения пользователя
     * @param summary суммаризация предыдущей истории диалога (может быть null)
     * @return результат выполнения запроса с разобранным ответом
     */
    override suspend fun sendMessagesWithSummary(
        messages: List<LLMMessage>,
        summary: String?
    ): Result<LLMResponse> {
        if (messages.isEmpty()) {
            return Result.failure(IllegalStateException("Empty messages"))
        }

        return safeApiCall(logger) {
            val lastMessageContent = messages.last().content
            logger.d("Отправка сообщения: \"${lastMessageContent.take(50)}${if (lastMessageContent.length > 50) "..." else ""}\"")

            // Получаем настройки из репозиториев
            val llmConfig = llmConfigRepository.getSettings()
            val providerSettings = providerSettingsRepository.getSettings()

            // Получаем эффективный системный промпт с учетом суммаризации
            val systemMessage = getEffectiveSystemPrompt(summary)

            // Базовые DTO-сообщения (system + входные доменные сообщения)
            val messagesDto = buildList {
                add(ChatMessageDTO(role = MessageRole.SYSTEM.key, content = systemMessage.content))
                addAll(messages.map { message ->
                    ChatMessageDTO(
                        role = message.role.key,
                        content = message.content
                    )
                })
            }.toMutableList()

            // Подготавливаем LLM tools (объединение из нескольких MCP, если задано)
            val llmTools: List<LlmToolSchemaDTO>? = buildLlmToolsIfEnabled(llmConfig)

            if (llmTools != null) {
                logger.i("🔧 LLM Tools enabled: ${llmTools.size} tools available")
                llmTools.forEach { tool ->
                    logger.i("   • ${tool.function.name}: ${tool.function.description}")
                }
            } else {
                logger.i("⚠️  LLM Tools disabled or unavailable")
            }

            // Выполняем первый запрос (с tools, если есть)
            var request = LLMChatRequestDTO(
                model = providerSettings.model,
                messages = messagesDto,
                temperature = llmConfig.temperature,
                maxTokens = llmConfig.maxTokens,
                topK = llmConfig.topK,
                topP = llmConfig.topP,
                minP = llmConfig.minP,
                topA = llmConfig.topA,
                seed = llmConfig.seed,
                tools = llmTools,
                apiKey = providerSettings.apiKey,
                apiUrl = providerSettings.apiUrl,
            )

            // Замеряем время начала первого запроса
            val startTime = System.currentTimeMillis()
            var response: LLMChatResponseDTO = openAIApi.sendRequest(request)
            var responseTime = System.currentTimeMillis() - startTime

            // Обработка возможных tool_calls (до MAX_TOOL_ROUNDS)
            var rounds = 0
            while (rounds < MAX_TOOL_ROUNDS) {
                val assistantMsg = response.choices.firstOrNull()?.message
                val toolCalls = assistantMsg?.toolCalls
                if (assistantMsg == null || toolCalls.isNullOrEmpty()) break

                // Добавляем сообщение ассистента (с tool_calls) в контекст
                messagesDto += assistantMsg

                // Выполняем каждый tool_call (маршрутизация на нужный MCP)
                val toolResults = handleToolCalls(toolCalls)

                messagesDto.addAll(toolResults)

                // Повторный запрос к LLM с добавленными tool-result
                val startTimeRound = System.currentTimeMillis()
                request = request.copy(messages = messagesDto)
                response = openAIApi.sendRequest(request)
                responseTime = System.currentTimeMillis() - startTimeRound
                rounds++
            }

            // Получаем содержимое финального сообщения
            val messageContent = response.choices.firstOrNull()?.message?.content
            if (messageContent.isNullOrBlank()) {
                logger.e("Пустой ответ от API после обработки tool_calls")
                throw Exception("Empty api response")
            }
            logger.d("Успешно получен ответ: \"${messageContent.take(50)}${if (messageContent.length > 50) "..." else ""}\"")

            // Создаём объект метрик из последнего ответа API
            val metrics = response.usage?.let { usageDto ->
                LLMUsage(
                    promptTokens = usageDto.promptTokens,
                    completionTokens = usageDto.completionTokens,
                    totalTokens = usageDto.totalTokens,
                    responseTimeMs = responseTime
                )
            }

            // Собираем финальный ответ доменной модели
            LLMResponse(
                id = response.id,
                choices = response.choices.map { choiceDto ->
                    LLMChoice(
                        index = choiceDto.index,
                        rawMessage = choiceDto.message.let { messageDto ->
                            LLMMessage(
                                role = MessageRole.getRole(messageDto.role),
                                content = messageDto.content ?: ""
                            )
                        },
                        parsedMessage = resultParser.parse(
                            (choiceDto.message.content ?: "") to llmConfig.responseFormat
                        ).getOrThrow(),
                        finishReason = choiceDto.finishReason
                    )
                },
                format = llmConfig.responseFormat,
                usage = metrics
            )
        }
    }

    /**
     * Готовит инструменты LLM (OpenAI-style tools), если фича-флаг включён.
     * Поддерживает множественные MCP через реестр оркестратора.
     *
     * Возвращает null, если инструменты недоступны или выключены.
     */
    private suspend fun buildLlmToolsIfEnabled(config: LLMConfig): List<LlmToolSchemaDTO>? {
        if (!config.enableMcpToolCalling) {
            logger.d("MCP Tool Calling disabled in config")
            return null
        }

        logger.d("🔍 Building LLM tools from MCP servers...")

        // 1) Попробуем использовать многосерверную конфигурацию
        val servers = runCatching { getMcpServers() }.getOrDefault(emptyList())
        logger.d("   Found ${servers.size} MCP servers in config")
        if (servers.isNotEmpty()) {
            // Сбор реестра (toolName -> wsUrl)
            runCatching { mcpRouter.rebuildRegistry(servers) }
                .onFailure { logger.e("Не удалось перестроить реестр MCP-инструментов", it) }

            // Собираем объединённый список инструментов
            val allTools = mutableListOf<ru.izhxx.aichallenge.mcp.domain.model.McpTool>()
            servers.forEach { s ->
                mcpRepository.listTools(s.url)
                    .onFailure { logger.e("Не удалось получить инструменты MCP с ${s.url}", it) }
                    .getOrNull()
                    ?.let { allTools.addAll(it) }
            }
            val distinct = allTools.distinctBy { it.name }
            if (distinct.isEmpty()) {
                logger.i("MCP-инструменты не получены или пусты, tools отключены (multi)")
                return null
            }
            logger.i("Подготовлено LLM tools (multi-MCP): ${distinct.size}")
            return toolsMapper.mapDomain(distinct)
        }

        // 2) Fallback: одиночный URL из старого репозитория
        val wsUrl = mcpConfigRepository.getWsUrl() ?: return null
        val tools = mcpRepository.listTools(wsUrl)
            .onFailure { logger.e("Не удалось получить список MCP-инструментов (single)", it) }
            .getOrNull()

        if (tools.isNullOrEmpty()) {
            logger.i("MCP-инструменты не получены или пусты, tools отключены (single)")
            return null
        }
        logger.i("Подготовлено LLM tools (single MCP): ${tools.size}")
        return toolsMapper.mapDomain(tools)
    }

    /**
     * Обрабатывает список tool_calls от ассистента и возвращает список сообщений role="tool".
     * Для каждого инструмента выполняется маршрутизация через McpRouter.
     */
    private suspend fun handleToolCalls(toolCalls: List<ToolCallDTO>): List<ChatMessageDTO> = coroutineScope {
        // Параллелим умеренно (по умолчанию столько же, сколько и toolCalls)
        val tasks = toolCalls.map { tc ->
            async {
                val name = tc.function.name
                val argsStr = tc.function.arguments
                val startTs = System.currentTimeMillis()

                val wsUrl = mcpRouter.resolve(name)
                val content =
                    if (wsUrl == null) {
                        buildErrorResult("tool not available: $name")
                    } else {
                        when (name) {
                            "github.list_user_repos" -> {
                                safeCall(name) {
                                    val args = json.parseToJsonElement(argsStr).jsonObject
                                    val username = args["username"]?.jsonPrimitive?.content
                                        ?: return@safeCall buildErrorResult("username is required")
                                    val perPage = args["per_page"]?.jsonPrimitive?.intOrNull() ?: 20
                                    val sort = args["sort"]?.jsonPrimitive?.content ?: "updated"
                                    val repos = mcpRepository
                                        .callListUserRepos(wsUrl, username, perPage, sort)
                                        .getOrElse { e ->
                                            logger.e("Ошибка MCP github.list_user_repos", e)
                                            return@safeCall buildErrorResult("mcp error: ${e.message}")
                                        }
                                    reposToJsonContent(repos)
                                }
                            }

                            "github.list_my_repos" -> {
                                safeCall(name) {
                                    val args = json.parseToJsonElement(argsStr).jsonObject
                                    val perPage = args["per_page"]?.jsonPrimitive?.intOrNull() ?: 20
                                    val sort = args["sort"]?.jsonPrimitive?.content ?: "updated"
                                    val visibility = args["visibility"]?.jsonPrimitive?.content ?: "all"
                                    val repos = mcpRepository
                                        .callListMyRepos(wsUrl, perPage, sort, visibility)
                                        .getOrElse { e ->
                                            logger.e("Ошибка MCP github.list_my_repos", e)
                                            return@safeCall buildErrorResult("mcp error: ${e.message}")
                                        }
                                    reposToJsonContent(repos)
                                }
                            }

                            else -> {
                                safeCall(name) {
                                    val args = runCatching { json.parseToJsonElement(argsStr) }.getOrNull()
                                        ?: return@safeCall buildErrorResult("invalid arguments json")
                                    mcpRepository
                                        .callTool(wsUrl, name, args)
                                        .mapCatching { resultEl -> json.encodeToString(resultEl) }
                                        .getOrElse { e ->
                                            logger.e("Ошибка MCP $name", e)
                                            buildErrorResult("mcp error: ${e.message}")
                                        }
                                }
                            }
                        }
                    }

                val elapsedMs = System.currentTimeMillis() - startTs
                logger.i("Tool-calling: $name finished in $elapsedMs ms")
                ChatMessageDTO(
                    role = "tool",
                    content = content,
                    toolCallId = tc.id
                )
            }
        }
        tasks.awaitAll()
    }

    /**
     * Формирует JSON-строку результата инструмента с массивом репозиториев.
     *
     * Формат:
     * {
     *   "items": [
     *     { "name": "...", "description": "...", "html_url": "...", "stargazers": 0, "language": "...", "updated_at": "..." }
     *   ]
     * }
     */
    private fun reposToJsonContent(repos: List<Repo>): String {
        val items = buildJsonArray {
            repos.forEach { repo ->
                add(
                    buildJsonObject {
                        put("name", JsonPrimitive(repo.name))
                        put("description", repo.description?.let { JsonPrimitive(it) } ?: JsonNull)
                        put("html_url", JsonPrimitive(repo.htmlUrl))
                        put("stargazers", JsonPrimitive(repo.stargazers))
                        put("language", repo.language?.let { JsonPrimitive(it) } ?: JsonNull)
                        put("updated_at", JsonPrimitive(repo.updatedAt))
                    }
                )
            }
        }
        val root = buildJsonObject {
            put("items", items)
        }
        return json.encodeToString(root)
    }

    /**
     * Утилита для безопасного выполнения блока и логирования ошибок по инструментам.
     */
    private inline fun safeCall(toolName: String, block: () -> String): String {
        return try {
            block()
        } catch (t: Throwable) {
            logger.e("Ошибка при выполнении инструмента $toolName", t)
            buildErrorResult("unexpected error: ${t.message}")
        }
    }

    /**
     * Формирует JSON-строку ошибки результата инструмента.
     */
    private fun buildErrorResult(message: String): String {
        val obj = buildJsonObject {
            put("error", JsonPrimitive(message))
        }
        return json.encodeToString(obj)
    }

    /**
     * Отправляет сообщения c кастомным системным промптом (обходит глобальный из настроек).
     * Используется для задач reminder.
     */
    override suspend fun sendMessagesWithCustomSystem(
        systemPrompt: String,
        messages: List<LLMMessage>,
        summary: String?
    ): Result<LLMResponse> {
        if (messages.isEmpty()) {
            return Result.failure(IllegalStateException("Empty messages"))
        }

        return safeApiCall(logger) {
            val lastMessageContent = messages.last().content
            logger.d("Отправка (custom system) сообщения: \"${lastMessageContent.take(50)}${if (lastMessageContent.length > 50) "..." else ""}\"")

            // Получаем настройки из репозиториев
            val llmConfig = llmConfigRepository.getSettings()
            val providerSettings = providerSettingsRepository.getSettings()

            // Формируем системный промпт с учётом формата и (опционально) суммаризации
            val basePrompt = """
                $systemPrompt
                ${FormatSystemPrompts.getFormatPrompt(llmConfig.responseFormat)}
            """.trimIndent()
            val finalPrompt = if (summary != null) {
                """
                $basePrompt

                $summary
                """.trimIndent()
            } else {
                basePrompt
            }
            val systemMessage = LLMMessage(
                role = MessageRole.SYSTEM,
                content = finalPrompt
            )

            // Готовим DTO-сообщения
            val messagesDto = buildList {
                add(ChatMessageDTO(role = MessageRole.SYSTEM.key, content = systemMessage.content))
                addAll(messages.map { message ->
                    ChatMessageDTO(
                        role = message.role.key,
                        content = message.content
                    )
                })
            }.toMutableList()

            // Подготавливаем LLM tools (объединение из нескольких MCP, если задано)
            val llmTools: List<LlmToolSchemaDTO>? = buildLlmToolsIfEnabled(llmConfig)

            if (llmTools != null) {
                logger.i("🔧 LLM Tools enabled: ${llmTools.size} tools available")
                llmTools.forEach { tool ->
                    logger.i("   • ${tool.function.name}: ${tool.function.description}")
                }
            } else {
                logger.i("⚠️  LLM Tools disabled or unavailable")
            }

            // Выполняем первый запрос (с tools, если есть)
            var request = LLMChatRequestDTO(
                model = providerSettings.model,
                messages = messagesDto,
                temperature = llmConfig.temperature,
                maxTokens = llmConfig.maxTokens,
                topK = llmConfig.topK,
                topP = llmConfig.topP,
                minP = llmConfig.minP,
                topA = llmConfig.topA,
                seed = llmConfig.seed,
                tools = llmTools,
                apiKey = providerSettings.apiKey,
                apiUrl = providerSettings.apiUrl,
            )

            val startTime = System.currentTimeMillis()
            var response: LLMChatResponseDTO = openAIApi.sendRequest(request)
            var responseTime = System.currentTimeMillis() - startTime

            // Обработка tool_calls
            var rounds = 0
            while (rounds < MAX_TOOL_ROUNDS) {
                val assistantMsg = response.choices.firstOrNull()?.message
                val toolCalls = assistantMsg?.toolCalls
                if (assistantMsg == null || toolCalls.isNullOrEmpty()) break

                messagesDto += assistantMsg

                val toolResults = handleToolCalls(toolCalls)

                messagesDto.addAll(toolResults)

                val startTimeRound = System.currentTimeMillis()
                request = request.copy(messages = messagesDto)
                response = openAIApi.sendRequest(request)
                responseTime = System.currentTimeMillis() - startTimeRound
                rounds++
            }

            val messageContent = response.choices.firstOrNull()?.message?.content
            if (messageContent.isNullOrBlank()) {
                logger.e("Пустой ответ от API после обработки tool_calls (custom system)")
                throw Exception("Empty api response")
            }
            logger.d("Успешно получен ответ (custom system): \"${messageContent.take(50)}${if (messageContent.length > 50) "..." else ""}\"")

            val metrics = response.usage?.let { usageDto ->
                LLMUsage(
                    promptTokens = usageDto.promptTokens,
                    completionTokens = usageDto.completionTokens,
                    totalTokens = usageDto.totalTokens,
                    responseTimeMs = responseTime
                )
            }

            LLMResponse(
                id = response.id,
                choices = response.choices.map { choiceDto ->
                    LLMChoice(
                        index = choiceDto.index,
                        rawMessage = choiceDto.message.let { messageDto ->
                            LLMMessage(
                                role = MessageRole.getRole(messageDto.role),
                                content = messageDto.content ?: ""
                            )
                        },
                        parsedMessage = resultParser.parse(
                            (choiceDto.message.content ?: "") to llmConfig.responseFormat
                        ).getOrThrow(),
                        finishReason = choiceDto.finishReason
                    )
                },
                format = llmConfig.responseFormat,
                usage = metrics
            )
        }
    }

    // Расширение для безопасного парсинга Int
    private fun JsonPrimitive.intOrNull(): Int? = try {
        this.content.toInt()
    } catch (_: Throwable) {
        null
    }
}
