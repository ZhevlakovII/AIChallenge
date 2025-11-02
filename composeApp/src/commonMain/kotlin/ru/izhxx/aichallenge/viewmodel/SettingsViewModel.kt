package ru.izhxx.aichallenge.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.data.api.OpenAIClient
import ru.izhxx.aichallenge.data.preferences.LLMSettingsStore
import ru.izhxx.aichallenge.domain.model.LLMUserSettings

/**
 * ViewModel для экрана настроек
 */
class SettingsViewModel(
    private val llmSettingsStore: LLMSettingsStore,
    private val openAIClient: OpenAIClient
) : ViewModel() {
    
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Загружает настройки LLM из хранилища
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val settings = llmSettingsStore.getSettings()
                _state.update { 
                    it.copy(
                        apiKey = settings.apiKey,
                        apiUrl = settings.apiUrl,
                        model = settings.model,
                        temperature = settings.temperature.toString(),
                        openaiProject = settings.openaiProject,
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
     * Обновляет значение OpenAI-Project в state
     */
    fun updateOpenaiProject(openaiProject: String) {
        _state.update { it.copy(openaiProject = openaiProject) }
    }
    
    /**
     * Сохраняет все настройки LLM в хранилище
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
        
        _state.update { it.copy(isLoading = true, isSaved = false, error = null) }
        
        val settings = LLMUserSettings(
            apiKey = currentState.apiKey.trim(),
            apiUrl = currentState.apiUrl.trim(),
            model = currentState.model.trim(),
            temperature = temperatureValue,
            openaiProject = currentState.openaiProject.trim()
        )
        
        viewModelScope.launch {
            try {
                llmSettingsStore.saveSettings(settings)
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
     * Сохраняет только API ключ в хранилище
     */
    fun saveApiKey() {
        val apiKey = state.value.apiKey.trim()
        if (apiKey.isBlank()) {
            _state.update { 
                it.copy(error = "API ключ не может быть пустым")
            }
            return
        }
        
        _state.update { it.copy(isLoading = true, isSaved = false, error = null) }
        
        viewModelScope.launch {
            try {
                llmSettingsStore.saveApiKey(apiKey)
                _state.update { 
                    it.copy(isLoading = false, isSaved = true)
                }
            } catch (e: Exception) {
                _state.update { 
                    it.copy(
                        error = "Не удалось сохранить API ключ: ${e.message}",
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
        val defaultSettings = LLMUserSettings()
        _state.update { 
            it.copy(
                apiKey = it.apiKey, // Сохраняем текущий API ключ
                apiUrl = defaultSettings.apiUrl,
                model = defaultSettings.model,
                temperature = defaultSettings.temperature.toString(),
                openaiProject = defaultSettings.openaiProject,
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
    val apiKey: String = "",
    val apiUrl: String = "",
    val model: String = "",
    val temperature: String = "",
    val openaiProject: String = "",
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)
