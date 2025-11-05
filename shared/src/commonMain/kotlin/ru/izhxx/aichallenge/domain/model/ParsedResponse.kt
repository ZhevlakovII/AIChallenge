package ru.izhxx.aichallenge.domain.model

/**
 * Модель парсённого ответа от LLM
 * Содержит извлечённые данные и информацию о формате
 */
data class ParsedResponse(
    val summary: String,
    val explanation: String,
    val code: String? = null,
    val references: List<String> = emptyList(),
    val originalText: String,
    val format: ResponseFormat,
    val isValid: Boolean = true,
    val validationError: String? = null,
)
