package ru.izhxx.aichallenge.domain.model.response

/**
 * Модель использования токенов и метрик запроса/ответа
 */
data class LLMUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val responseTimeMs: Long
)
