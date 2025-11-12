package ru.izhxx.aichallenge.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.data.parser.MessageContentBuilder
import ru.izhxx.aichallenge.data.parser.ResponseParser
import ru.izhxx.aichallenge.domain.model.LLMException
import ru.izhxx.aichallenge.domain.model.Message
import ru.izhxx.aichallenge.domain.model.MessageType
import ru.izhxx.aichallenge.domain.model.openai.ChatMessage
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository
import ru.izhxx.aichallenge.domain.repository.LLMPromptSettingsRepository
import ru.izhxx.aichallenge.domain.repository.LLMProviderSettingsRepository
import java.util.UUID

class ChatViewModel(
    private val llmClientRepository: LLMClientRepository,
    private val llmProviderSettingsRepositoryImpl: LLMProviderSettingsRepository,
    private val llmPromptSettingsRepositoryImpl: LLMPromptSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    // Добавляем приветственное сообщение при инициализации и проверяем наличие API ключа
    init {
        viewModelScope.launch {
            val welcomeMessage = Message(
                id = UUID.randomUUID().toString(),
                text = "Привет! Я Senior Android Developer и готов ответить на твои вопросы по Android-разработке.",
                type = MessageType.TECHNICAL
            )
            addMessage(welcomeMessage)

            // Проверяем и загружаем актуальные настройки
            refreshSettings()

            // Проверяем наличие API ключа
            checkApiKeyConfigured()
        }
    }

    /**
     * Проверяет и обновляет актуальные настройки LLM
     */
    private fun refreshSettings() {
        viewModelScope.launch {
            try {
                llmPromptSettingsRepositoryImpl.getSettings()
                llmProviderSettingsRepositoryImpl.getSettings()
            } catch (e: Exception) {
                // Логируем ошибку, но не прерываем работу
                e.printStackTrace()
            }
        }
    }

    /**
     * Проверяет, настроен ли API ключ
     */
    private fun checkApiKeyConfigured() {
        viewModelScope.launch {
            if (llmProviderSettingsRepositoryImpl.getApiKey().isEmpty()) {
                // Если API ключ не настроен, показываем сообщение и предлагаем открыть настройки
                addMessage(
                    Message(
                        id = UUID.randomUUID().toString(),
                        text = "Для начала работы необходимо настроить API ключ OpenAI в настройках.",
                        type = MessageType.TECHNICAL
                    )
                )
                _state.update {
                    it.copy(apiKeyConfigured = false)
                }
            } else {
                _state.update {
                    it.copy(apiKeyConfigured = true)
                }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || state.value.isLoading) return

        // Проверяем и обновляем актуальные настройки перед отправкой
        refreshSettings()

        checkApiKeyConfigured()
        // Проверяем, настроен ли API ключ
        if (!state.value.apiKeyConfigured) {
            _state.update {
                it.copy(
                    llmException = LLMException(
                        errorCode = "ERROR_NO_API_KEY",
                        userFriendlyMessage = "API ключ не настроен",
                        detailedMessage = "API ключ OpenAI не был найден в настройках. Пожалуйста, перейдите в настройки и добавьте ваш API ключ."
                    ),
                    hasError = true
                )
            }
            return
        }

        // Добавляем сообщение пользователя
        val userMessage = Message(
            id = UUID.randomUUID().toString(),
            text = text,
            type = MessageType.USER
        )
        addMessage(userMessage)

        // Устанавливаем состояние загрузки
        _state.update { it.copy(isLoading = true, inputText = "", llmException = null, hasError = false) }

        performSendMessage(text)
    }

    /**
     * Повторно отправляет последнее сообщение при ошибке
     */
    fun retryLastMessage() {
        val currentState = state.value
        
        // Ищем последнее сообщение пользователя
        val lastUserMessage = currentState.messages
            .filter { it.type != MessageType.TECHNICAL }
            .lastOrNull { it.type == MessageType.USER }
        
        if (lastUserMessage != null) {
            _state.update { it.copy(isLoading = true, llmException = null, hasError = false) }
            performSendMessage(lastUserMessage.text)
        }
    }

    /**
     * Выполняет отправку сообщения
     */
    private fun performSendMessage(text: String) {
        viewModelScope.launch {
            try {
                // Формируем список предыдущих сообщений для контекста
                // Исключаем технические сообщения
                val messages = state.value.messages
                    .filter { it.type != MessageType.TECHNICAL }
                    .map {
                        ChatMessage(
                            role = if (it.type == MessageType.USER) "user" else "assistant",
                            content = it.text
                        )
                    }
                    .toMutableList()

                // Отправляем запрос с полной историей
                llmClientRepository.sendMessage(messages)
                    .fold(
                        onSuccess = { parsedResponse ->
                            // Получаем отображаемый текст из парсённого ответа
                            val displayText = ResponseParser.getDisplayText(parsedResponse)

                            // Строим структурированное содержимое сообщения
                            val messageContent = MessageContentBuilder.buildMessageContent(
                                parsedResponse = parsedResponse,
                                format = parsedResponse.format
                            )

                            // Добавляем ответ от агента с информацией о формате и метриках
                            val assistantMessage = Message(
                                id = UUID.randomUUID().toString(),
                                text = displayText,
                                type = MessageType.ASSISTANT,
                                responseFormat = parsedResponse.format,
                                metrics = parsedResponse.metrics,
                                content = messageContent
                            )
                            addMessage(assistantMessage)

                            _state.update { it.copy(isLoading = false, hasError = false, llmException = null) }
                        },
                        onFailure = { error ->
                            val llmException = error as? LLMException
                                ?: LLMException(
                                    errorCode = "ERROR_GENERIC",
                                    userFriendlyMessage = "Ошибка обработки ответа",
                                    detailedMessage = error.message ?: "Неизвестная ошибка"
                                )
                            
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    hasError = true,
                                    llmException = llmException
                                )
                            }
                        }
                    )
            } catch (e: Exception) {
                val llmException = LLMException(
                    errorCode = "ERROR_GENERIC",
                    userFriendlyMessage = "Неизвестная ошибка",
                    detailedMessage = e.message ?: "Неизвестная ошибка"
                )
                
                _state.update {
                    it.copy(
                        isLoading = false,
                        hasError = true,
                        llmException = llmException
                    )
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun clearError() {
        _state.update { it.copy(llmException = null, hasError = false) }
    }

    /**
     * Очищает историю чата и добавляет приветственное сообщение
     */
    fun clearHistory() {
        viewModelScope.launch {
            val welcomeMessage = Message(
                id = UUID.randomUUID().toString(),
                text = "Привет! Я Senior Android Developer и готов ответить на твои вопросы по Android-разработке.",
                type = MessageType.TECHNICAL
            )
            _state.update {
                it.copy(messages = listOf(welcomeMessage), llmException = null, hasError = false, inputText = "")
            }
        }
    }

    private fun addMessage(message: Message) {
        _state.update {
            it.copy(messages = it.messages + message)
        }
    }
}

data class ChatState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val llmException: LLMException? = null,
    val hasError: Boolean = false,
    val apiKeyConfigured: Boolean = false
) {
    // Для совместимости оставляем свойство error
    val error: String? get() = if (hasError) llmException?.getShortErrorInfo() else null
}
