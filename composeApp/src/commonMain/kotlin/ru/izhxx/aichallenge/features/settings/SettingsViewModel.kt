package ru.izhxx.aichallenge.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.domain.model.config.LLMConfig
import ru.izhxx.aichallenge.domain.model.config.ProviderSettings
import ru.izhxx.aichallenge.domain.model.config.ResponseFormat
import ru.izhxx.aichallenge.domain.repository.LLMConfigRepository
import ru.izhxx.aichallenge.domain.repository.ProviderSettingsRepository
import ru.izhxx.aichallenge.features.settings.state.SettingsState

/**
 * ViewModel для экрана настроек
 */
class SettingsViewModel(
    private val providerSettingsStore: ProviderSettingsRepository,
    private val lLMConfigRepository: LLMConfigRepository,
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
                val lLMConfig = lLMConfigRepository.getSettings()
                
                _state.update { 
                    it.copy(
                        apiKey = providerSettings.apiKey,
                        apiUrl = providerSettings.apiUrl,
                        model = providerSettings.model,
                        temperature = lLMConfig.temperature.toString(),
                        maxTokens = lLMConfig.maxTokens.toString(),
                        responseFormat = lLMConfig.responseFormat,
                        systemPrompt = lLMConfig.systemPrompt,
                        topK = lLMConfig.topK.toString(),
                        topP = lLMConfig.topP.toString(),
                        minP = lLMConfig.minP.toString(),
                        topA = lLMConfig.topA.toString(),
                        seed = lLMConfig.seed.toString(),
                        enableMcpToolCalling = lLMConfig.enableMcpToolCalling,
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
     * Обновляет значение topK в state
     */
    fun updateTopK(topK: String) {
        _state.update { it.copy(topK = topK) }
    }

    /**
     * Обновляет значение topP в state
     */
    fun updateTopP(topP: String) {
        _state.update { it.copy(topP = topP) }
    }

    /**
     * Обновляет значение minP в state
     */
    fun updateMinP(minP: String) {
        _state.update { it.copy(minP = minP) }
    }

    /**
     * Обновляет значение topA в state
     */
    fun updateTopA(topA: String) {
        _state.update { it.copy(topA = topA) }
    }

    /**
     * Обновляет значение seed в state
     */
    fun updateSeed(seed: String) {
        _state.update { it.copy(seed = seed) }
    }

    /**
     * Обновляет флаг использования MCP tools (function calling)
     */
    fun updateEnableMcpToolCalling(enabled: Boolean) {
        _state.update { it.copy(enableMcpToolCalling = enabled) }
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
        val temperatureValue = currentState.temperature.toDoubleOrNull()

        if (temperatureValue == null || temperatureValue < 0.0 || temperatureValue > 1.0) {
            _state.update {
                it.copy(error = "Температура должна быть числом от 0.0 до 1.0")
            }
            return
        }

        // Проверяем и парсим maxTokens
        val maxTokensValue = currentState.maxTokens.toIntOrNull()

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

        // Проверяем и парсим topK
        val topKValue = currentState.topK.toIntOrNull()

        if (topKValue == null || topKValue <= 0) {
            _state.update {
                it.copy(error = "Top-K должен быть положительным числом")
            }
            return
        }

        // Проверяем и парсим topP
        val topPValue = currentState.topP.toDoubleOrNull()

        if (topPValue == null || topPValue < 0.0 || topPValue > 1.0) {
            _state.update {
                it.copy(error = "Top-P должен быть числом от 0.0 до 1.0")
            }
            return
        }

        // Проверяем и парсим minP
        val minPValue = currentState.minP.toDoubleOrNull()

        if (minPValue == null || minPValue < 0.0 || minPValue > 1.0) {
            _state.update {
                it.copy(error = "Min-P должен быть числом от 0.0 до 1.0")
            }
            return
        }

        // Проверяем и парсим topA
        val topAValue = currentState.topA.toDoubleOrNull()

        if (topAValue == null || topAValue < 0.0) {
            _state.update {
                it.copy(error = "Top-A должен быть положительным числом")
            }
            return
        }

        // Проверяем и парсим seed
        val seedValue = currentState.seed.toLongOrNull()

        if (seedValue == null) {
            _state.update {
                it.copy(error = "Seed должен быть целым числом")
            }
            return
        }

        _state.update { it.copy(isLoading = true, isSaved = false, error = null) }

        val providerSettings = ProviderSettings(
            apiKey = currentState.apiKey.trim(),
            apiUrl = currentState.apiUrl.trim(),
            model = currentState.model.trim(),
        )

        val lLMConfig = LLMConfig(
            temperature = temperatureValue,
            maxTokens = maxTokensValue,
            responseFormat = currentState.responseFormat,
            systemPrompt = currentState.systemPrompt.trim(),
            topK = topKValue,
            topP = topPValue,
            minP = minPValue,
            topA = topAValue,
            seed = seedValue,
            enableMcpToolCalling = currentState.enableMcpToolCalling
        )

        viewModelScope.launch {
            try {
                providerSettingsStore.saveSettings(providerSettings)
                lLMConfigRepository.saveSettings(lLMConfig)
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
        val defaultProviderSettings = ProviderSettings.default()
        val defaultLLMConfig = LLMConfig.default()

        _state.update {
            it.copy(
                apiKey = it.apiKey, // Сохраняем текущий API ключ
                apiUrl = defaultProviderSettings.apiUrl,
                model = defaultProviderSettings.model,
                temperature = defaultLLMConfig.temperature.toString(),
                maxTokens = defaultLLMConfig.maxTokens.toString(),
                responseFormat = defaultLLMConfig.responseFormat,
                systemPrompt = defaultLLMConfig.systemPrompt,
                topK = defaultLLMConfig.topK.toString(),
                topP = defaultLLMConfig.topP.toString(),
                minP = defaultLLMConfig.minP.toString(),
                topA = defaultLLMConfig.topA.toString(),
                seed = defaultLLMConfig.seed.toString(),
                enableMcpToolCalling = defaultLLMConfig.enableMcpToolCalling,
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
