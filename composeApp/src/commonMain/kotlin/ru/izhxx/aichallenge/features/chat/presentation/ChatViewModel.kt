package ru.izhxx.aichallenge.features.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.error.DomainException
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.repository.LLMConfigRepository
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
    private val llmConfigRepository: LLMConfigRepository,
    private val responseMapper: ChatResponseMapper
) : ViewModel() {

    // Состояние UI
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    // История сообщений для контекста LLM (не включает технические сообщения)
    private val messageHistory = mutableListOf<LLMMessage>()

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
        when (event) {
            is ChatEvent.SendMessage -> handleSendMessage(event.text)
            is ChatEvent.RetryLastMessage -> handleRetryLastMessage()
            is ChatEvent.ClearHistory -> handleClearHistory()
            is ChatEvent.UpdateInputText -> handleUpdateInputText(event.text)
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
    private fun handleSendMessage(text: String) {
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
        messageHistory.add(userLlmMessage)

        // Устанавливаем состояние загрузки и очищаем ошибки
        _state.update {
            it.copy(
                isLoading = true,
                inputText = "",
                error = null
            )
        }

        // Выполняем отправку сообщения
        performSendMessage(text, requestId)
    }

    /**
     * Выполняет отправку сообщения в LLM
     */
    private fun performSendMessage(text: String, requestId: String) {
        viewModelScope.launch {
            try {
                // Отправляем сообщение с полной историей
                sendMessageUseCase(text, messageHistory).fold(
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
                            content = response.choices.firstOrNull()?.rawMessage?.content ?: ""
                        )
                        messageHistory.add(assistantLlmMessage)

                        // Обновляем состояние
                        _state.update { it.copy(isLoading = false, error = null) }
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
                        val domainError = if (error is DomainException) {
                            error // Если это уже DomainException, используем его как есть
                        } else {
                            DomainException(
                                errorMessage,
                                error
                            ) // Иначе создаем новый с оригинальной ошибкой как причиной
                        }

                        _state.update { it.copy(isLoading = false, error = domainError) }
                    }
                )
            } catch (e: Exception) {
                // Обрабатываем непредвиденные ошибки
                val domainError = DomainException("Неизвестная ошибка при обработке запроса", e)

                _state.update { it.copy(isLoading = false, error = domainError) }
            }
        }
    }

    /**
     * Обрабатывает повторную отправку последнего сообщения
     */
    private fun handleRetryLastMessage() {
        val currentState = state.value

        // Если есть предыдущие сообщения пользователя
        if (messageHistory.isNotEmpty()) {
            // Ищем последнее сообщение пользователя
            val lastUserMessageIndex = messageHistory.indexOfLast { it.role == MessageRole.USER }

            if (lastUserMessageIndex != -1) {
                val lastUserMessage = messageHistory[lastUserMessageIndex]
                val requestId = UUID.randomUUID().toString()

                // Удаляем все сообщения после последнего сообщения пользователя
                if (lastUserMessageIndex < messageHistory.size - 1) {
                    messageHistory.subList(lastUserMessageIndex + 1, messageHistory.size).clear()
                }

                _state.update { it.copy(isLoading = true, error = null) }

                // Повторно отправляем сообщение
                performSendMessage(lastUserMessage.content, requestId)
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
    private fun handleClearHistory() {
        viewModelScope.launch {
            // Очищаем историю сообщений
            messageHistory.clear()

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
