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
import ru.izhxx.aichallenge.domain.rag.RagSettingsRepository
import ru.izhxx.aichallenge.domain.rag.RagSettings
import ru.izhxx.aichallenge.domain.rag.RerankSettings
import ru.izhxx.aichallenge.domain.rag.RerankMode
import ru.izhxx.aichallenge.domain.rag.CutoffMode

/**
 * ViewModel для экрана настроек
 */
class SettingsViewModel(
    private val providerSettingsStore: ProviderSettingsRepository,
    private val lLMConfigRepository: LLMConfigRepository,
    private val ragSettingsRepository: RagSettingsRepository,
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
                val ragSettings = ragSettingsRepository.getSettings()
                
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
                        // RAG
                        ragEnabled = ragSettings.enabled,
                        ragIndexPath = ragSettings.indexPath.orEmpty(),
                        ragTopK = ragSettings.topK.toString(),
                        ragMinScore = ragSettings.minScore.toString(),
                        ragMaxContextTokens = ragSettings.maxContextTokens.toString(),
                        // ReranK (2-й этап)
                        ragRerankMode = ragSettings.rerank.mode,
                        ragCandidateK = ragSettings.rerank.candidateK.toString(),
                        ragMmrLambda = ragSettings.rerank.mmrLambda.toString(),
                        ragCutoffMode = ragSettings.rerank.cutoffMode,
                        ragMinRerankScore = ragSettings.rerank.minRerankScore?.toString().orEmpty(),
                        ragQuantileQ = ragSettings.rerank.quantileQ.toString(),
                        ragZScore = ragSettings.rerank.zScore.toString(),
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

    // ======== RAG setters ========
    fun updateRagEnabled(enabled: Boolean) {
        _state.update { it.copy(ragEnabled = enabled) }
    }
    fun updateRagIndexPath(path: String) {
        _state.update { it.copy(ragIndexPath = path) }
    }
    fun updateRagTopK(value: String) {
        _state.update { it.copy(ragTopK = value) }
    }
    fun updateRagMinScore(value: String) {
        _state.update { it.copy(ragMinScore = value) }
    }
    fun updateRagMaxContextTokens(value: String) {
        _state.update { it.copy(ragMaxContextTokens = value) }
    }

    // ======== RAG RERANK setters ========
    fun updateRagRerankMode(mode: RerankMode) {
        _state.update { it.copy(ragRerankMode = mode) }
    }
    fun updateRagCandidateK(value: String) {
        _state.update { it.copy(ragCandidateK = value) }
    }
    fun updateRagMmrLambda(value: String) {
        _state.update { it.copy(ragMmrLambda = value) }
    }
    fun updateRagCutoffMode(mode: CutoffMode) {
        _state.update { it.copy(ragCutoffMode = mode) }
    }
    fun updateRagMinRerankScore(value: String) {
        _state.update { it.copy(ragMinRerankScore = value) }
    }
    fun updateRagQuantileQ(value: String) {
        _state.update { it.copy(ragQuantileQ = value) }
    }
    fun updateRagZScore(value: String) {
        _state.update { it.copy(ragZScore = value) }
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

        // Валидация RAG (требуется только если включен)
        if (currentState.ragEnabled) {
            val ragTopKValue = currentState.ragTopK.toIntOrNull()
            if (ragTopKValue == null || ragTopKValue <= 0) {
                _state.update { it.copy(error = "RAG: Top-K должен быть положительным числом") }
                return
            }
            val ragMinScoreValue = currentState.ragMinScore.toDoubleOrNull()
            if (ragMinScoreValue == null || ragMinScoreValue < 0.0 || ragMinScoreValue > 1.0) {
                _state.update { it.copy(error = "RAG: Min score должен быть числом от 0.0 до 1.0") }
                return
            }
            val ragMaxCtxValue = currentState.ragMaxContextTokens.toIntOrNull()
            if (ragMaxCtxValue == null || ragMaxCtxValue <= 0) {
                _state.update { it.copy(error = "RAG: Max context tokens должен быть положительным числом") }
                return
            }
            if (currentState.ragIndexPath.isBlank()) {
                _state.update { it.copy(error = "RAG: путь к индексу не может быть пустым") }
                return
            }

            // Валидация RERANK
            val candidateKValue = currentState.ragCandidateK.toIntOrNull()
            if (candidateKValue == null || candidateKValue <= 0) {
                _state.update { it.copy(error = "RAG: candidateK должен быть положительным числом") }
                return
            }
            val mmrLambdaValue = currentState.ragMmrLambda.toDoubleOrNull()
            if (mmrLambdaValue == null || mmrLambdaValue < 0.0 || mmrLambdaValue > 1.0) {
                _state.update { it.copy(error = "RAG: mmrLambda должен быть числом от 0.0 до 1.0") }
                return
            }
            when (currentState.ragCutoffMode) {
                CutoffMode.Static -> {
                    if (currentState.ragMinRerankScore.isNotBlank()) {
                        val v = currentState.ragMinRerankScore.toDoubleOrNull()
                        if (v == null || v < 0.0 || v > 1.0) {
                            _state.update { it.copy(error = "RAG: Static minRerankScore должен быть числом 0.0..1.0 или пустым") }
                            return
                        }
                    }
                }
                CutoffMode.Quantile -> {
                    val q = currentState.ragQuantileQ.toDoubleOrNull()
                    if (q == null || q < 0.0 || q > 1.0) {
                        _state.update { it.copy(error = "RAG: Quantile q должен быть числом 0.0..1.0") }
                        return
                    }
                }
                CutoffMode.ZScore -> {
                    val z = currentState.ragZScore.toDoubleOrNull()
                    if (z == null) {
                        _state.update { it.copy(error = "RAG: ZScore threshold должен быть числом") }
                        return
                    }
                }
            }
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

        // Готовим RagSettings к сохранению (значения валидированы при включённом RAG)
        val ragDefaults = RagSettings()
        val ragTopKValue = currentState.ragTopK.toIntOrNull()
        val ragMinScoreValue = currentState.ragMinScore.toDoubleOrNull()
        val ragMaxCtxValue = currentState.ragMaxContextTokens.toIntOrNull()
        val candidateKValue = currentState.ragCandidateK.toIntOrNull()
        val mmrLambdaValue = currentState.ragMmrLambda.toDoubleOrNull()
        val minRerankScoreValue = currentState.ragMinRerankScore.toDoubleOrNull()
        val quantileQValue = currentState.ragQuantileQ.toDoubleOrNull()
        val zScoreValue = currentState.ragZScore.toDoubleOrNull()

        val rerank = RerankSettings(
            mode = currentState.ragRerankMode,
            candidateK = candidateKValue ?: 16,
            mmrLambda = mmrLambdaValue ?: 0.5,
            cutoffMode = currentState.ragCutoffMode,
            minRerankScore = if (currentState.ragCutoffMode == CutoffMode.Static) minRerankScoreValue else null,
            quantileQ = quantileQValue ?: 0.2,
            zScore = zScoreValue ?: -0.5
        )

        val ragSettings = ragDefaults.copy(
            enabled = currentState.ragEnabled,
            indexPath = currentState.ragIndexPath.trim().ifBlank { null },
            topK = ragTopKValue ?: ragDefaults.topK,
            minScore = ragMinScoreValue ?: ragDefaults.minScore,
            maxContextTokens = ragMaxCtxValue ?: ragDefaults.maxContextTokens,
            rerank = rerank
        )

        viewModelScope.launch {
            try {
                providerSettingsStore.saveSettings(providerSettings)
                lLMConfigRepository.saveSettings(lLMConfig)
                ragSettingsRepository.saveSettings(ragSettings)
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
                // Сбрасываем RAG поля к дефолтам UI (не сохраняем автоматически)
                ragEnabled = false,
                ragIndexPath = "",
                ragTopK = RagSettings().topK.toString(),
                ragMinScore = RagSettings().minScore.toString(),
                ragMaxContextTokens = RagSettings().maxContextTokens.toString(),
                // RERANK дефолты
                ragRerankMode = RerankMode.None,
                ragCandidateK = "16",
                ragMmrLambda = "0.5",
                ragCutoffMode = CutoffMode.Quantile,
                ragMinRerankScore = "",
                ragQuantileQ = "0.2",
                ragZScore = "-0.5",
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
