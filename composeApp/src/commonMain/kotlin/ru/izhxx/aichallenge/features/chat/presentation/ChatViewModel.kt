package ru.izhxx.aichallenge.features.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.domain.model.ChatMetrics
import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.error.DomainException
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.repository.LLMConfigRepository
import ru.izhxx.aichallenge.domain.repository.MetricsCacheRepository
import ru.izhxx.aichallenge.domain.usecase.CompressDialogHistoryUseCase
import ru.izhxx.aichallenge.features.chat.domain.usecase.CheckApiKeyConfigurationUseCase
import ru.izhxx.aichallenge.features.chat.domain.usecase.SendMessageUseCase
import ru.izhxx.aichallenge.features.chat.presentation.mapper.ChatResponseMapper
import ru.izhxx.aichallenge.features.chat.presentation.model.ChatEvent
import ru.izhxx.aichallenge.features.chat.presentation.model.ChatUiMessage
import ru.izhxx.aichallenge.features.chat.presentation.model.ChatUiState
import java.util.UUID

/**
 * ViewModel для экрана чата с использованием паттерна MVI
 */
class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase,
    private val checkApiKeyConfigurationUseCase: CheckApiKeyConfigurationUseCase,
    private val compressDialogHistoryUseCase: CompressDialogHistoryUseCase,
    private val llmConfigRepository: LLMConfigRepository,
    private val metricsCacheRepository: MetricsCacheRepository,
    private val responseMapper: ChatResponseMapper
) : ViewModel() {

    // Текущая суммаризация истории диалога
    private var currentSummary: String? = null

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
        // Инициализация: добавляем приветственное сообщение и проверяем настройки
        viewModelScope.launch {
            // Добавляем приветственное сообщение
            val welcomeMessage = responseMapper.createTechnicalUiMessage(
                "Привет! Я Senior Android Developer и готов ответить на твои вопросы по Android-разработке."
            )
            addUiMessage(welcomeMessage)

            // Проверяем наличие API ключа
            refreshApiKeyConfiguration()
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

                    // Сжимаем историю после добавления ответа ассистента
                    // Создаем копию списка для безопасной обработки
                    val historySnapshot = buildList(messageHistory.value.size) {
                        addAll(messageHistory.value)
                    }
                    val originalHistorySize = historySnapshot.size

                    // Используем копию для сжатия
                    val compressionResult = compressDialogHistoryUseCase(historySnapshot)
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

                        // Обновляем историю сообщений
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
                            val keepMessages = subList(0, lastUserMessageIndex + 1).toMutableList()
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
     * Очищает историю чата и добавляет приветственное сообщение
     */
    private suspend fun handleClearHistory() {
        viewModelScope.launch {
            // Очищаем историю сообщений и суммаризацию
            messageHistory.update {
                it.apply {
                    clear()
                }
            }
            currentSummary = null

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
