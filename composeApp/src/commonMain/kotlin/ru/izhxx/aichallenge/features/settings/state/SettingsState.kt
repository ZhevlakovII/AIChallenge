package ru.izhxx.aichallenge.features.settings.state

import ru.izhxx.aichallenge.domain.model.config.ResponseFormat

/**
 * Состояние для экрана настроек
 */
data class SettingsState(
    // Настройки провайдера
    val apiKey: String = "",
    val apiUrl: String = "",
    val model: String = "",

    // Настройки промпта
    val temperature: String = "0.7",
    val maxTokens: String = "4096",
    val responseFormat: ResponseFormat = ResponseFormat.MARKDOWN,
    val systemPrompt: String = "",

    // Расширенные настройки
    val topK: String = "40",
    val topP: String = "0.95",
    val minP: String = "0.05",
    val topA: String = "0.0",
    val seed: String = "0",

    // Состояние UI
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)