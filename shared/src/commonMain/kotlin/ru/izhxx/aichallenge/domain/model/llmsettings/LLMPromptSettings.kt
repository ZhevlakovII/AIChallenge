package ru.izhxx.aichallenge.domain.model.llmsettings

import kotlinx.serialization.Serializable
import ru.izhxx.aichallenge.domain.model.LLMDefaults
import ru.izhxx.aichallenge.domain.model.ResponseFormat

/**
 * Настройки взаимодействия с LLM
 * Содержит параметры, которые влияют на промпт и генерацию ответов
 */
@Serializable
data class LLMPromptSettings(
    /**
     * Температура генерации (влияет на случайность ответов)
     * Диапазон значений: 0.0 - 1.0
     * Более низкие значения делают ответы более детерминированными
     * Более высокие значения делают ответы более случайными и творческими
     */
    val temperature: Double = 0.7,

    /**
     * Формат ответа LLM
     */
    val responseFormat: ResponseFormat = ResponseFormat.XML,

    /**
     * Системный промпт, определяющий роль и поведение LLM
     */
    val systemPrompt: String = LLMDefaults.DEFAULT_SYSTEM_PROMPT
)
