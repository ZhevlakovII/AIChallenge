package ru.izhxx.aichallenge.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.izhxx.aichallenge.domain.model.ChatMetrics
import ru.izhxx.aichallenge.domain.model.summary.DialogSummaryMetrics

/**
 * Репозиторий для кеширования и обновления метрик чата
 */
interface MetricsCacheRepository {

    /**
     * Flow с текущими метриками чата
     */
    val metricsFlow: Flow<ChatMetrics>

    /**
     * Добавляет токены основного запроса LLM в метрики
     * @param promptTokens токены запроса
     * @param completionTokens токены ответа
     * @param responseTimeMs время ответа в миллисекундах
     */
    suspend fun addLLMTokens(promptTokens: Int, completionTokens: Int, responseTimeMs: Long)
    
    /**
     * Добавляет токены суммаризации в метрики
     * @param summaryMetrics метрики суммаризации
     */
    suspend fun addSummaryTokens(summaryMetrics: DialogSummaryMetrics)
    
    /**
     * Обновляет счетчик сообщений
     * @param messageCount новое количество сообщений
     */
    suspend fun updateMessageCount(messageCount: Int)
    
    /**
     * Увеличивает счетчик сжатий
     */
    suspend fun incrementCompressionCount()
    
    /**
     * Сбрасывает все метрики
     */
    suspend fun resetMetrics()
}
