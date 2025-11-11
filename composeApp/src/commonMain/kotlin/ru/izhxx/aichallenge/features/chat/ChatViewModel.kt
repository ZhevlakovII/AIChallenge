package ru.izhxx.aichallenge.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.data.parser.ResponseParser
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

    // Флаг, показывающий необходимость открыть экран настроек
    private val _navigateToSettings = MutableStateFlow(false)
    val navigateToSettings: StateFlow<Boolean> = _navigateToSettings

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

    /**
     * Запускает навигацию на экран настроек
     */
    fun navigateToSettings() {
        _navigateToSettings.value = true
    }

    /**
     * Сбрасывает флаг навигации на экран настроек
     */
    fun onNavigatedToSettings() {
        _navigateToSettings.value = false
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
                    error = "API ключ OpenAI не настроен. Пожалуйста, настройте его в настройках."
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
        _state.update { it.copy(isLoading = true, inputText = "", error = null) }

        viewModelScope.launch {
            try {
                // Формируем список предыдущих сообщений для контекста
                // Исключаем технические сообщения и пропускаем текущее сообщение пользователя
                val messages = state.value.messages
                    .dropLast(1)  // Убираем только что добавленное сообщение пользователя
                    .filter { it.type != MessageType.TECHNICAL }  // Исключаем технические сообщения
                    .map {
                        ChatMessage(
                            role = if (it.type == MessageType.USER) "user" else "assistant",
                            content = it.text
                        )
                    }
                    .toMutableList()

                messages.add(
                    ChatMessage(
                        role = "user",
                        content = text
                    )
                )

                // Отправляем запрос с полной историей
                llmClientRepository.sendMessage(messages)
                    .fold(
                        onSuccess = { parsedResponse ->
                            // Получаем отображаемый текст из парсённого ответа
                            val displayText = ResponseParser.getDisplayText(parsedResponse)

                            // Добавляем ответ от агента с информацией о формате и метриках
                            val assistantMessage = Message(
                                id = UUID.randomUUID().toString(),
                                text = displayText,
                                type = MessageType.ASSISTANT,
                                responseFormat = parsedResponse.format,
                                metrics = parsedResponse.metrics
                            )
                            addMessage(assistantMessage)

                            _state.update { it.copy(isLoading = false) }
                        },
                        onFailure = { error ->
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Ошибка при обработке ответа: ${error.message ?: "Неизвестная ошибка"}"
                                )
                            }
                        }
                    )
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Ошибка: ${e.message ?: "Неизвестная ошибка"}"
                    )
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
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
                it.copy(messages = listOf(welcomeMessage), error = null, inputText = "")
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
    val error: String? = null,
    val apiKeyConfigured: Boolean = false
)
