package ru.izhxx.aichallenge.domain.model.summary

/**
 * Метрики суммаризации диалога
 * Содержит информацию о количестве токенов и времени выполнения суммаризации
 */
data class DialogSummaryMetrics(
    /**
     * Количество токенов в запросе
     */
    val promptTokens: Int,
    
    /**
     * Количество токенов в ответе
     */
    val completionTokens: Int,
    
    /**
     * Общее количество токенов
     */
    val totalTokens: Int,
    
    /**
     * Время выполнения запроса в миллисекундах
     */
    val responseTimeMs: Long
)
