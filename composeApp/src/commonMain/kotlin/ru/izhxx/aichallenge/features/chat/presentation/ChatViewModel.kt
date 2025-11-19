package ru.izhxx.aichallenge.features.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.domain.model.ChatMetrics
import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.error.DomainException
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.repository.DialogPersistenceRepository
import ru.izhxx.aichallenge.domain.repository.LLMConfigRepository
import ru.izhxx.aichallenge.domain.repository.MetricsCacheRepository
import ru.izhxx.aichallenge.domain.usecase.CompressDialogHistoryUseCase
import ru.izhxx.aichallenge.features.chat.domain.usecase.CheckApiKeyConfigurationUseCase
import ru.izhxx.aichallenge.features.chat.domain.usecase.SendMessageUseCase
import ru.izhxx.aichallenge.features.chat.presentation.mapper.ChatResponseMapper
import ru.izhxx.aichallenge.features.chat.presentation.model.ChatEvent
import ru.izhxx.aichallenge.features.chat.presentation.model.ChatUiMessage
import ru.izhxx.aichallenge.features.chat.presentation.model.ChatUiState
import ru.izhxx.aichallenge.features.chat.presentation.model.MessageContent
import java.util.UUID
import ru.izhxx.aichallenge.mcp.domain.usecase.EnsureMcpConnectedUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.GetSavedMcpUrlUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.GetGithubUserReposUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.GetMyGithubReposUseCase
import ru.izhxx.aichallenge.mcp.domain.usecase.GetMcpToolsUseCase

/**
 * ViewModel для экрана чата с использованием паттерна MVI
 */
class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase,
    private val checkApiKeyConfigurationUseCase: CheckApiKeyConfigurationUseCase,
    private val compressDialogHistoryUseCase: CompressDialogHistoryUseCase,
    private val llmConfigRepository: LLMConfigRepository,
    private val metricsCacheRepository: MetricsCacheRepository,
    private val dialogPersistenceRepository: DialogPersistenceRepository,
    private val responseMapper: ChatResponseMapper,
    // MCP
    private val ensureMcpConnectedUseCase: EnsureMcpConnectedUseCase,
    private val getSavedMcpUrlUseCase: GetSavedMcpUrlUseCase,
    private val getGithubUserReposUseCase: GetGithubUserReposUseCase,
    private val getMyGithubReposUseCase: GetMyGithubReposUseCase,
    private val getMcpToolsUseCase: GetMcpToolsUseCase
) : ViewModel() {

    // Для логирования
    private val logger = Logger.forClass(this)

    // Текущая суммаризация истории диалога
    private var currentSummary: String? = null

    // ID текущего диалога
    private var currentDialogId: String? = null
    
    // Счетчик сообщений в истории для текущего диалога
    private var currentMessageCounter = 0

    // Состояние UI
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    // Метрики использования (получаем из репозитория)
    val metrics: StateFlow<ChatMetrics> = metricsCacheRepository.metricsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChatMetrics()
    )

    // История сообщений для контекста LLM (не включает технические сообщения)
    private val messageHistory = MutableStateFlow<MutableList<LLMMessage>>(mutableListOf())

    init {
        // Инициализация: создаем или восстанавливаем диалог, добавляем приветственное сообщение и проверяем настройки
        viewModelScope.launch {
            // Добавляем приветственное сообщение
            val welcomeMessage = responseMapper.createTechnicalUiMessage(
                "Привет! Я Senior Android Developer и готов ответить на твои вопросы по Android-разработке."
            )
            addUiMessage(welcomeMessage)

            // Создаем новый диалог или восстанавливаем последний
            initializeOrRestoreDialog()

            // Проверяем наличие API ключа
            refreshApiKeyConfiguration()
        }
    }

    /**
     * Инициализирует новый диалог или восстанавливает существующий
     */
    private suspend fun initializeOrRestoreDialog() {
        try {
            // Проверяем наличие сохраненных диалогов
            val dialogs = dialogPersistenceRepository.getAllDialogs()

            if (dialogs.isNotEmpty()) {
                // Берем самый последний диалог (сортировка уже выполнена в репозитории)
                val lastDialog = dialogs.first()
                currentDialogId = lastDialog.id

                logger.d("Восстановление диалога: $currentDialogId")

                // Восстанавливаем контекстные сообщения для LLM
                val contextMessages =
                    dialogPersistenceRepository.getContextMessages(currentDialogId!!)

                // Обновляем историю сообщений для LLM
                messageHistory.update {
                    it.apply {
                        clear()
                        addAll(contextMessages)
                    }
                }

                // Восстанавливаем суммаризацию
                currentSummary = dialogPersistenceRepository.getLatestSummary(currentDialogId!!)

                logger.d("Восстановлен диалог: $currentDialogId с ${contextMessages.size} сообщениями контекста")

                // Восстанавливаем полную историю для UI
                val historyMessages =
                    dialogPersistenceRepository.getHistoryMessages(currentDialogId!!)

                // Преобразуем сохраненные сообщения в UI-модели и обновляем UI
                if (historyMessages.isNotEmpty()) {
                    val uiMessages = historyMessages.map { message ->
                        when (message.role) {
                            MessageRole.USER -> responseMapper.createUserUiMessage(
                                message.content,
                                false, // нет ошибок для восстановленных сообщений
                                UUID.randomUUID().toString()
                            )

                            MessageRole.ASSISTANT -> ChatUiMessage.AssistantMessage(
                                id = UUID.randomUUID().toString(),
                                content = MessageContent.Plain(message.content),
                                metadata = null // у восстановленных сообщений нет метаданных
                            )

                            else -> responseMapper.createTechnicalUiMessage(message.content)
                        }
                    }

                    // Обновляем состояние UI
                    _state.update { it.copy(messages = it.messages.plus(uiMessages)) }

                    logger.d("Восстановлено ${uiMessages.size} UI-сообщений из истории")
                    
                    // Инициализируем счетчик сообщений для текущего диалога
                    currentMessageCounter = historyMessages.size
                    logger.d("Инициализирован счетчик сообщений: $currentMessageCounter")
                }
            } else {
                // Если нет сохраненных диалогов, создаем новый
                currentDialogId = dialogPersistenceRepository.createNewDialog()
                logger.d("Создан новый диалог: $currentDialogId")
            }
        } catch (e: Exception) {
            // В случае ошибки создаем новый диалог и продолжаем работу
            logger.e("Ошибка при инициализации диалога", e)
            currentDialogId = UUID.randomUUID().toString()
        }
    }

    /**
     * Сохраняет одно сообщение в историю и контекст
     * Использует отдельный счетчик для порядка сообщений, который увеличивается последовательно
     * для каждого нового сообщения в рамках диалога
     */
    private suspend fun saveSingleMessage(
        message: LLMMessage,
        promptTokens: Int? = null,
        completionTokens: Int? = null,
        totalTokens: Int? = null,
        responseTimeMs: Long? = null
    ) {
        withContext(Dispatchers.IO) {
            // Увеличиваем счетчик для текущего сообщения
            currentMessageCounter++

            currentDialogId?.let { dialogId ->
                try {
                    val title =
                        messageHistory.value.firstOrNull { it.role == MessageRole.USER }?.content?.take(
                            50
                        ) ?: "Диалог"

                    dialogPersistenceRepository.saveMessage(
                        dialogId = dialogId,
                        title = title,
                        message = message,
                        orderInDialog = currentMessageCounter,  // Используем счетчик для порядка
                        promptTokens = promptTokens,
                        completionTokens = completionTokens,
                        totalTokens = totalTokens,
                        responseTimeMs = responseTimeMs
                    )

                    logger.d("Сохранено сообщение в диалог $dialogId, порядок: $currentMessageCounter")
                } catch (e: Exception) {
                    logger.e("Ошибка при сохранении сообщения", e)
                }
            }
        }
    }

    /**
     * Обрабатывает события UI
     */
    fun processEvent(event: ChatEvent) {
        viewModelScope.launch {
            when (event) {
                is ChatEvent.SendMessage -> handleSendMessage(event.text)
                is ChatEvent.RetryLastMessage -> handleRetryLastMessage()
                is ChatEvent.ClearHistory -> handleClearHistory()
                is ChatEvent.UpdateInputText -> handleUpdateInputText(event.text)
            }
        }
    }

    /**
     * Проверяет и обновляет настройки API ключа
     */
    private fun refreshApiKeyConfiguration() {
        viewModelScope.launch {
            try {
                // Обновляем настройки LLM
                llmConfigRepository.getSettings()

                // Проверяем наличие API ключа
                val isApiKeyConfigured = checkApiKeyConfigurationUseCase()

                _state.update { it.copy(apiKeyConfigured = isApiKeyConfigured) }

                // Если API ключ не настроен, показываем сообщение
                if (!isApiKeyConfigured) {
                    val noApiKeyMessage = responseMapper.createTechnicalUiMessage(
                        "Для начала работы необходимо настроить API ключ OpenAI в настройках."
                    )
                    addUiMessage(noApiKeyMessage)
                }
            } catch (e: Exception) {
                // Логируем ошибку, но не прерываем работу
                e.printStackTrace()
            }
        }
    }

    /**
     * Обрабатывает отправку сообщения
     */
    private suspend fun handleSendMessage(text: String) {
        if (text.isBlank() || state.value.isLoading) return

        // Перехват MCP-команд (не зависит от LLM API ключа)
        if (handleMcpCommand(text)) return

        // Обновляем состояние и проверяем настройки перед отправкой
        refreshApiKeyConfiguration()

        // Проверяем, настроен ли API ключ
        if (!state.value.apiKeyConfigured) {
            _state.update {
                it.copy(
                    error = DomainException(
                        "API ключ OpenAI не был найден в настройках. Пожалуйста, перейдите в настройки и добавьте ваш API ключ."
                    )
                )
            }
            return
        }

        // Создаём ID для пары запрос-ответ
        val requestId = UUID.randomUUID().toString()

        // Добавляем сообщение пользователя в UI
        val userUiMessage =
            responseMapper.createUserUiMessage(text, state.value.error != null, requestId)
        addUiMessage(userUiMessage)

        // Добавляем сообщение пользователя в историю для контекста LLM
        val userLlmMessage = LLMMessage(
            role = MessageRole.USER,
            content = text
        )

        messageHistory.update {
            it.apply {
                add(userLlmMessage)
            }
        }

        // Сохраняем сообщение пользователя
        saveSingleMessage(
            message = userLlmMessage
        )

        // Устанавливаем состояние загрузки и очищаем ошибки
        _state.update {
            it.copy(
                isLoading = true,
                inputText = "",
                error = null
            )
        }

        // Выполняем отправку сообщения с учетом суммаризации
        performSendMessage(text, requestId, currentSummary)
    }

    /**
     * Выполняет отправку сообщения в LLM
     */
    private suspend fun performSendMessage(text: String, requestId: String, summary: String?) {
        try {
            // Отправляем сообщение с историей и суммаризацией
            sendMessageUseCase.invoke(text, messageHistory.value, summary).fold(
                onSuccess = { response ->
                    // Преобразуем ответ в UI-сообщение
                    val assistantUiMessage = responseMapper.mapLLMResponseToUiMessage(
                        choice = response.choices.first(),
                        responseFormat = response.format,
                        usage = response.usage
                    )

                    // Добавляем сообщение ассистента в UI
                    addUiMessage(assistantUiMessage)

                    // Добавляем сообщение ассистента в историю для контекста LLM
                    val assistantLlmMessage = LLMMessage(
                        role = MessageRole.ASSISTANT,
                        content = response.choices.firstOrNull()?.rawMessage?.content.orEmpty()
                    )

                    messageHistory.update {
                        it.apply {
                            add(assistantLlmMessage)
                        }
                    }

                    // Сохраняем сообщение ассистента с метриками
                    saveSingleMessage(
                        message = assistantLlmMessage,
                        promptTokens = response.usage?.promptTokens,
                        completionTokens = response.usage?.completionTokens,
                        totalTokens = response.usage?.totalTokens,
                        responseTimeMs = response.usage?.responseTimeMs
                    )

                    // Сжимаем историю после добавления ответа ассистента
                    // Создаем копию списка для безопасной обработки
                    val historySnapshot = buildList(messageHistory.value.size) {
                        addAll(messageHistory.value)
                    }
                    val originalHistorySize = historySnapshot.size

                    // Используем копию для сжатия и передаем текущую суммаризацию для инкрементальной суммаризации
                    val compressionResult =
                        compressDialogHistoryUseCase(historySnapshot, currentSummary)
                    val newSummary = compressionResult.first
                    val compressedMessages = compressionResult.second
                    val summaryMetrics = compressionResult.third

                    // Обновляем текущую суммаризацию
                    val wasCompressed = newSummary != null
                    if (wasCompressed) {
                        // Добавляем уведомление о суммаризации
                        val compressionMessage = responseMapper.createTechnicalUiMessage(
                            "История диалога была сжата. $originalHistorySize сообщений заменены на суммаризацию."
                        )
                        addUiMessage(compressionMessage)

                        // Обновляем текущую суммаризацию
                        currentSummary = newSummary

                        // Сохраняем суммаризацию с метриками
                        currentDialogId?.let { dialogId ->
                            // Удаляем все сообщения для контекста из БД
                            logger.d("Удаляем все сообщения для контекста после суммаризации")
                            dialogPersistenceRepository.clearContextMessages(dialogId)
                            logger.d("Сохранение суммаризации после сжатия: $dialogId")

                            // Сохраняем суммаризацию с метриками
                            currentSummary?.let { summary ->
                                dialogPersistenceRepository.saveSummary(
                                    dialogId,
                                    summary,
                                    summaryMetrics
                                )
                            }
                        }

                        // Обновляем историю сообщений в памяти
                        messageHistory.update {
                            it.apply {
                                clear()
                                addAll(compressedMessages)
                            }
                        }

                        // Обновляем метрики по сжатию в репозитории
                        metricsCacheRepository.incrementCompressionCount()
                        metricsCacheRepository.updateMessageCount(messageHistory.value.size)

                        // Добавляем метрики суммаризации в общую статистику, если они доступны
                        summaryMetrics?.let { metrics ->
                            metricsCacheRepository.addSummaryTokens(metrics)
                        }
                    }

                    // Обновляем состояние
                    _state.update { it.copy(isLoading = false, error = null) }

                    // Обновляем метрики использования в репозитории
                    response.usage?.let { usage ->
                        metricsCacheRepository.addLLMTokens(
                            promptTokens = usage.promptTokens,
                            completionTokens = usage.completionTokens,
                            responseTimeMs = usage.responseTimeMs
                        )
                        metricsCacheRepository.updateMessageCount(messageHistory.value.size)
                    }
                },
                onFailure = { error ->
                    // Обрабатываем ошибку
                    val errorMessage = when {
                        error.message?.contains("API ключ не настроен") == true -> {
                            "API ключ не настроен. Перейдите в настройки для настройки API ключа."
                        }

                        error.message?.contains("network", ignoreCase = true) == true -> {
                            "Ошибка сети. Проверьте подключение к Интернету."
                        }

                        else -> {
                            "Произошла ошибка при отправке сообщения: ${error.message ?: "причина не указана"}"
                        }
                    }

                    // Создаем DomainException с сообщением и причиной
                    val domainError = // Если это уже DomainException, используем его как есть
                        error as? DomainException
                            ?: DomainException(
                                errorMessage,
                                error
                            ) // Иначе создаем новый с оригинальной ошибкой как причиной

                    _state.update { it.copy(isLoading = false, error = domainError) }

                    // Проверяем, можно ли автоматически повторить запрос при ошибках сети
                    if (error.message?.contains("network", ignoreCase = true) == true) {
                        // Добавляем сообщение о повторной попытке
                        val retryMessage = responseMapper.createTechnicalUiMessage(
                            "Произошла ошибка сети. Автоматическая повторная попытка через 2 секунды..."
                        )
                        addUiMessage(retryMessage)

                        // Повторяем запрос через 2 секунды
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(2000)
                            handleRetryLastMessage()
                        }
                    }
                }
            )
        } catch (e: Exception) {
            // Обрабатываем непредвиденные ошибки
            val domainError = DomainException("Неизвестная ошибка при обработке запроса", e)

            _state.update { it.copy(isLoading = false, error = domainError) }

            // Добавляем возможность повторить запрос
            val retryMessage = responseMapper.createTechnicalUiMessage(
                "Произошла непредвиденная ошибка. Пожалуйста, используйте кнопку повтора на сообщении или отправьте запрос еще раз."
            )
            addUiMessage(retryMessage)
        }
    }

    /**
     * Обрабатывает MCP-команды:
     * - "/repos <username>" — публичные репозитории пользователя GitHub
     * - "/myrepos" — репозитории аутентифицированного пользователя
     * - "/tools" — список доступных инструментов MCP
     *
     * Возвращает true, если команда обработана и дальнейшая LLM-отправка не требуется.
     */
    private suspend fun handleMcpCommand(text: String): Boolean {
        val trimmed = text.trim()
        val isRepos = trimmed.startsWith("/repos ")
        val isMyRepos = trimmed == "/myrepos"
        val isTools = trimmed == "/tools"

        if (!isRepos && !isMyRepos && !isTools) return false

        // Включаем индикатор загрузки
        _state.update { it.copy(isLoading = true, error = null) }

        // Получаем/используем сохранённый URL MCP
        val wsUrl = try {
            getSavedMcpUrlUseCase()
        } catch (e: Exception) {
            _state.update { it.copy(isLoading = false) }
            val msg = responseMapper.createTechnicalUiMessage(
                "MCP URL не настроен. Откройте экран MCP и сохраните адрес сервера."
            )
            addUiMessage(msg)
            return true
        }

        // Проверяем соединение
        val connected = ensureMcpConnectedUseCase(wsUrl).isSuccess
        if (!connected) {
            _state.update { it.copy(isLoading = false) }
            val msg = responseMapper.createTechnicalUiMessage(
                "Не удалось подключиться к MCP. Проверьте, что сервер запущен и URL корректен."
            )
            addUiMessage(msg)
            return true
        }

        try {
            when {
                isRepos -> {
                    val username = trimmed.removePrefix("/repos").trim()
                    if (username.isBlank()) {
                        val msg = responseMapper.createTechnicalUiMessage(
                            "Использование: /repos <username>"
                        )
                        addUiMessage(msg)
                        _state.update { it.copy(isLoading = false) }
                        return true
                    }
                    val result = getGithubUserReposUseCase(wsUrl, username)
                    result.fold(
                        onSuccess = { repos ->
                            val textOut = buildReposPlainList(repos, header = "Публичные репозитории $username:")
                            addUiMessage(
                                ChatUiMessage.AssistantMessage(
                                    id = UUID.randomUUID().toString(),
                                    content = MessageContent.Plain(textOut),
                                    metadata = null
                                )
                            )
                            _state.update { it.copy(isLoading = false) }
                        },
                        onFailure = { e ->
                            val msg = responseMapper.createTechnicalUiMessage(
                                "Ошибка при получении репозиториев: ${e.message ?: "неизвестная ошибка"}"
                            )
                            addUiMessage(msg)
                            _state.update { it.copy(isLoading = false) }
                        }
                    )
                }
                isMyRepos -> {
                    val result = getMyGithubReposUseCase(wsUrl)
                    result.fold(
                        onSuccess = { repos ->
                            val textOut = buildReposPlainList(repos, header = "Мои репозитории:")
                            addUiMessage(
                                ChatUiMessage.AssistantMessage(
                                    id = UUID.randomUUID().toString(),
                                    content = MessageContent.Plain(textOut),
                                    metadata = null
                                )
                            )
                            _state.update { it.copy(isLoading = false) }
                        },
                        onFailure = { e ->
                            val msg = responseMapper.createTechnicalUiMessage(
                                "Ошибка при получении моих репозиториев: ${e.message ?: "неизвестная ошибка"}. " +
                                        "Убедитесь, что на сервере задан GITHUB_TOKEN."
                            )
                            addUiMessage(msg)
                            _state.update { it.copy(isLoading = false) }
                        }
                    )
                }
                isTools -> {
                    val result = getMcpToolsUseCase(wsUrl)
                    result.fold(
                        onSuccess = { tools ->
                            val listText = buildString {
                                appendLine("Доступные инструменты MCP (${tools.size}):")
                                tools.forEach { t ->
                                    append("- ")
                                    append(t.name)
                                    t.description?.let {
                                        append(" — ")
                                        append(it)
                                    }
                                    appendLine()
                                }
                            }
                            addUiMessage(
                                ChatUiMessage.AssistantMessage(
                                    id = UUID.randomUUID().toString(),
                                    content = MessageContent.Plain(listText),
                                    metadata = null
                                )
                            )
                            _state.update { it.copy(isLoading = false) }
                        },
                        onFailure = { e ->
                            val msg = responseMapper.createTechnicalUiMessage(
                                "Ошибка при получении инструментов MCP: ${e.message ?: "неизвестная ошибка"}"
                            )
                            addUiMessage(msg)
                            _state.update { it.copy(isLoading = false) }
                        }
                    )
                }
            }
        } catch (e: Exception) {
            val msg = responseMapper.createTechnicalUiMessage(
                "Ошибка обработки MCP-команды: ${e.message ?: "неизвестная ошибка"}"
            )
            addUiMessage(msg)
            _state.update { it.copy(isLoading = false) }
        }

        return true
    }

    /**
     * Формирует простой текстовый список репозиториев.
     */
    private fun buildReposPlainList(
        repos: List<ru.izhxx.aichallenge.domain.model.github.Repo>,
        header: String
    ): String {
        return buildString {
            appendLine(header)
            if (repos.isEmpty()) {
                appendLine("Список пуст.")
            } else {
                repos.forEach { r ->
                    append("- ")
                    append(r.name)
                    r.description?.let { desc ->
                        if (desc.isNotBlank()) {
                            append(" — ")
                            append(desc.trim())
                        }
                    }
                    append(" (⭐")
                    append(r.stargazers)
                    r.language?.let { append(", ").append(it) }
                    append(") ")
                    append(r.htmlUrl)
                    appendLine()
                }
            }
        }
    }

    /**
     * Обрабатывает повторную отправку последнего сообщения
     */
    private suspend fun handleRetryLastMessage() {
        // Если есть предыдущие сообщения пользователя
        if (messageHistory.value.isNotEmpty()) {
            // Ищем последнее сообщение пользователя
            val lastUserMessageIndex =
                messageHistory.value.indexOfLast { it.role == MessageRole.USER }

            if (lastUserMessageIndex != -1) {
                val lastUserMessage = messageHistory.value[lastUserMessageIndex]
                val requestId = UUID.randomUUID().toString()

                // Удаляем все сообщения после последнего сообщения пользователя
                if (lastUserMessageIndex < messageHistory.value.size - 1) {
                    messageHistory.update {
                        it.apply {
                            // Сохраняем сообщения до индекса включительно, удаляем остальные
                            val keepMessages =
                                subList(0, lastUserMessageIndex + 1).toMutableList()
                            clear()
                            addAll(keepMessages)
                        }
                    }
                }

                _state.update { it.copy(isLoading = true, error = null) }

                // Повторно отправляем сообщение с текущей суммаризацией
                performSendMessage(lastUserMessage.content, requestId, currentSummary)
            }
        }
    }

    /**
     * Обновляет текст ввода
     */
    private fun handleUpdateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    /**
     * Очищает историю чата, создает новый диалог и добавляет приветственное сообщение
     */
    private fun handleClearHistory() {
        viewModelScope.launch {
            // Очищаем историю сообщений и суммаризацию
            messageHistory.update {
                it.apply {
                    clear()
                }
            }
            currentSummary = null
            
            // Сбрасываем счетчик сообщений
            currentMessageCounter = 0
            logger.d("Сброшен счетчик сообщений")

            // Создаем новый диалог
            try {
                logger.d("Создание нового диалога после очистки истории")
                currentDialogId = dialogPersistenceRepository.createNewDialog()
            } catch (e: Exception) {
                logger.e("Ошибка при создании нового диалога", e)
                currentDialogId = UUID.randomUUID().toString()
            }

            // Создаем приветственное сообщение
            val welcomeMessage = responseMapper.createTechnicalUiMessage(
                "Привет! Я Senior Android Developer и готов ответить на твои вопросы по Android-разработке."
            )

            // Обновляем состояние
            _state.update {
                it.copy(
                    messages = listOf(welcomeMessage),
                    inputText = "",
                    error = null,
                    isLoading = false
                )
            }

            // Сбрасываем метрики
            metricsCacheRepository.resetMetrics()
        }
    }

    /**
     * Добавляет UI-сообщение в список сообщений
     */
    private fun addUiMessage(message: ChatUiMessage) {
        _state.update {
            it.copy(messages = it.messages + message)
        }
    }
}
