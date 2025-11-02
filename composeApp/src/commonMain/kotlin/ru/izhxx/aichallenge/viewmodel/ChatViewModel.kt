package ru.izhxx.aichallenge.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.data.api.OpenAIClient
import ru.izhxx.aichallenge.domain.model.Message
import java.util.UUID

class ChatViewModel(private val openAIClient: OpenAIClient) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    
    // Флаг, показывающий необходимость открыть экран настроек
    private val _navigateToSettings = MutableStateFlow(false)
    val navigateToSettings: StateFlow<Boolean> = _navigateToSettings

    // Добавляем приветственное сообщение при инициализации и проверяем наличие API ключа
    init {
        addMessage(
            Message(
                id = UUID.randomUUID().toString(),
                text = "Привет! Я Senior Android Developer и готов ответить на твои вопросы по Android-разработке.",
                isFromUser = false
            )
        )
        
        // Проверяем наличие API ключа
        checkApiKeyConfigured()
    }
    
    /**
     * Проверяет, настроен ли API ключ
     */
    private fun checkApiKeyConfigured() {
        viewModelScope.launch {
            if (!openAIClient.isApiKeyConfigured()) {
                // Если API ключ не настроен, показываем сообщение и предлагаем открыть настройки
                addMessage(
                    Message(
                        id = UUID.randomUUID().toString(),
                        text = "Для начала работы необходимо настроить API ключ OpenAI в настройках.",
                        isFromUser = false
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
            isFromUser = true
        )
        addMessage(userMessage)
        
        // Устанавливаем состояние загрузки
        _state.update { it.copy(isLoading = true, inputText = "", error = null) }
        
        viewModelScope.launch {
            // Отправляем запрос к API
            openAIClient.sendMessage(text)
                .fold(
                    onSuccess = { response ->
                        // Добавляем ответ от агента
                        addMessage(
                            Message(
                                id = UUID.randomUUID().toString(),
                                text = response,
                                isFromUser = false
                            )
                        )
                        _state.update { it.copy(isLoading = false) }
                    },
                    onFailure = { error ->
                        _state.update { 
                            it.copy(
                                isLoading = false,
                                error = error.message ?: "Неизвестная ошибка"
                            ) 
                        }
                    }
                )
        }
    }
    
    fun updateInputText(text: String) {
        _state.update { it.copy(inputText = text) }
    }
    
    fun clearError() {
        _state.update { it.copy(error = null) }
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
