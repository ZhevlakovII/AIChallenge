package ru.izhxx.aichallenge.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.domain.model.ResponseFormat
import ru.izhxx.aichallenge.domain.model.llmsettings.LLMPromptSettings
import ru.izhxx.aichallenge.domain.model.llmsettings.LLMProviderSettings
import ru.izhxx.aichallenge.domain.repository.LLMPromptSettingsRepository
import ru.izhxx.aichallenge.domain.repository.LLMProviderSettingsRepository

/**
 * ViewModel для экрана настроек
 */
class SettingsViewModel(
    private val providerSettingsStore: LLMProviderSettingsRepository,
    private val promptSettingsStore: LLMPromptSettingsRepository,
) : ViewModel() {
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Загружает настройки LLM из хранилищ
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val providerSettings = providerSettingsStore.getSettings()
                val promptSettings = promptSettingsStore.getSettings()
                
                _state.update { 
                    it.copy(
                        apiKey = providerSettings.apiKey,
                        apiUrl = providerSettings.apiUrl,
                        model = providerSettings.model,
                        openaiProject = providerSettings.openaiProject,
                        temperature = promptSettings.temperature.toString(),
                        maxTokens = promptSettings.maxTokens.toString(),
                        responseFormat = promptSettings.responseFormat,
                        systemPrompt = promptSettings.systemPrompt,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Не удалось загрузить настройки LLM: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Обновляет значение API ключа в state
     */
    fun updateApiKey(apiKey: String) {
        _state.update { it.copy(apiKey = apiKey) }
    }
    
    /**
     * Обновляет значение URL API в state
     */
    fun updateApiUrl(apiUrl: String) {
        _state.update { it.copy(apiUrl = apiUrl) }
    }
    
    /**
     * Обновляет значение модели в state
     */
    fun updateModel(model: String) {
        _state.update { it.copy(model = model) }
    }
    
    /**
     * Обновляет значение температуры в state
     */
    fun updateTemperature(temperature: String) {
        _state.update { it.copy(temperature = temperature) }
    }
    
    /**
     * Обновляет значение maxTokens в state
     */
    fun updateMaxTokens(maxTokens: String) {
        _state.update { it.copy(maxTokens = maxTokens) }
    }
    
    /**
     * Обновляет значение OpenAI-Project в state
     */
    fun updateOpenaiProject(openaiProject: String) {
        _state.update { it.copy(openaiProject = openaiProject) }
    }
    
    /**
     * Обновляет значение формата ответа в state
     */
    fun updateResponseFormat(format: ResponseFormat) {
        _state.update { it.copy(responseFormat = format) }
    }
    
    /**
     * Обновляет значение системного промпта в state
     */
    fun updateSystemPrompt(prompt: String) {
        _state.update { it.copy(systemPrompt = prompt) }
    }
    
    /**
     * Сохраняет все настройки LLM в хранилища
     */
    fun saveSettings() {
        val currentState = state.value
        
        // Проверяем API ключ
        if (currentState.apiKey.isBlank()) {
            _state.update { 
                it.copy(error = "API ключ не может быть пустым")
            }
            return
        }
        
        // Проверяем URL API
        if (currentState.apiUrl.isBlank()) {
            _state.update { 
                it.copy(error = "URL API не может быть пустым")
            }
            return
        }
        
        // Проверяем модель
        if (currentState.model.isBlank()) {
            _state.update { 
                it.copy(error = "Модель не может быть пустой")
            }
            return
        }
        
        // Проверяем и парсим температуру
        val temperatureValue = try {
            currentState.temperature.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
        
        if (temperatureValue == null || temperatureValue < 0.0 || temperatureValue > 1.0) {
            _state.update { 
                it.copy(error = "Температура должна быть числом от 0.0 до 1.0")
            }
            return
        }
        
        // Проверяем и парсим maxTokens
        val maxTokensValue = try {
            currentState.maxTokens.toIntOrNull()
        } catch (e: Exception) {
            null
        }
        
        if (maxTokensValue == null || maxTokensValue <= 0) {
            _state.update { 
                it.copy(error = "Max tokens должен быть положительным числом")
            }
            return
        }
        
        // Проверяем системный промпт
        if (currentState.systemPrompt.isBlank()) {
            _state.update { 
                it.copy(error = "Системный промпт не может быть пустым")
            }
            return
        }
        
        _state.update { it.copy(isLoading = true, isSaved = false, error = null) }
        
        val providerSettings = LLMProviderSettings(
            apiKey = currentState.apiKey.trim(),
            apiUrl = currentState.apiUrl.trim(),
            model = currentState.model.trim(),
            openaiProject = currentState.openaiProject.trim()
        )
        
        val promptSettings = LLMPromptSettings(
            temperature = temperatureValue,
            maxTokens = maxTokensValue,
            responseFormat = currentState.responseFormat,
            systemPrompt = currentState.systemPrompt.trim()
        )
        
        viewModelScope.launch {
            try {
                providerSettingsStore.saveSettings(providerSettings)
                promptSettingsStore.saveSettings(promptSettings)
                _state.update { 
                    it.copy(isLoading = false, isSaved = true)
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Не удалось сохранить настройки LLM: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Восстанавливает настройки по умолчанию
     */
    fun restoreDefaults() {
        val defaultProviderSettings = LLMProviderSettings()
        val defaultPromptSettings = LLMPromptSettings()
        
        _state.update { 
            it.copy(
                apiKey = it.apiKey, // Сохраняем текущий API ключ
                apiUrl = defaultProviderSettings.apiUrl,
                model = defaultProviderSettings.model,
                openaiProject = defaultProviderSettings.openaiProject,
                temperature = defaultPromptSettings.temperature.toString(),
                maxTokens = defaultPromptSettings.maxTokens.toString(),
                responseFormat = defaultPromptSettings.responseFormat,
                systemPrompt = defaultPromptSettings.systemPrompt,
                error = null
            )
        }
    }
    
    /**
     * Очищает состояние сохранения
     */
    fun clearSavedState() {
        _state.update { it.copy(isSaved = false) }
    }
    
    /**
     * Очищает ошибку
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

/**
 * Состояние для экрана настроек
 */
data class SettingsState(
    // Настройки провайдера
    val apiKey: String = "",
    val apiUrl: String = "",
    val model: String = "",
    val openaiProject: String = "",
    
    // Настройки промпта
    val temperature: String = "0.7",
    val maxTokens: String = "4096",
    val responseFormat: ResponseFormat = ResponseFormat.XML,
    val systemPrompt: String = "",
    
    // Состояние UI
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)
