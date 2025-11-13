package ru.izhxx.aichallenge.domain.model.response

import ru.izhxx.aichallenge.domain.model.config.ResponseFormat

/**
 * Модель ответа от LLM
 * Содержит варианты ответа и статистику использования токенов
 */
data class LLMResponse(
    val id: String,
    val choices: List<LLMChoice>,
    val format: ResponseFormat,
    val usage: LLMUsage?
)
