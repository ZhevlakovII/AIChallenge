package ru.izhxx.aichallenge.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.domain.model.ChatMetrics
import ru.izhxx.aichallenge.domain.model.summary.DialogSummaryMetrics
import ru.izhxx.aichallenge.domain.repository.MetricsCacheRepository

/**
 * Реализация репозитория для кеширования и обновления метрик чата
 */
class MetricsCacheRepositoryImpl : MetricsCacheRepository {

    // Создаем логгер
    private val logger = Logger.forClass(this)

    // Внутренний MutableStateFlow для хранения метрик
    private val _metrics = MutableStateFlow(ChatMetrics())
    
    // Публичный доступ к метрикам через неизменяемый StateFlow
    override val metricsFlow: StateFlow<ChatMetrics> = _metrics.asStateFlow()
    
    /**
     * Добавляет токены основного запроса LLM в метрики
     * @param promptTokens токены запроса
     * @param completionTokens токены ответа
     * @param responseTimeMs время ответа в миллисекундах
     */
    override suspend fun addLLMTokens(promptTokens: Int, completionTokens: Int, responseTimeMs: Long) {
        logger.d("Добавлены токены основного LLM: промпт=$promptTokens, ответ=$completionTokens, время=${responseTimeMs}мс")
        
        _metrics.value = _metrics.value.let { current ->
            current.copy(
                totalPromptTokens = current.totalPromptTokens + promptTokens,
                totalCompletionTokens = current.totalCompletionTokens + completionTokens,
                totalTokens = current.totalTokens + promptTokens + completionTokens,
                averageResponseTime = if (current.responseCount > 0) 
                    (current.averageResponseTime * current.responseCount + responseTimeMs) / 
                        (current.responseCount + 1)
                else 
                    responseTimeMs,
                responseCount = current.responseCount + 1
            )
        }
    }
    
    /**
     * Добавляет токены суммаризации в метрики
     * @param summaryMetrics метрики суммаризации
     */
    override suspend fun addSummaryTokens(summaryMetrics: DialogSummaryMetrics) {
        logger.d("Добавлены токены суммаризации: промпт=${summaryMetrics.promptTokens}, " +
                "ответ=${summaryMetrics.completionTokens}, время=${summaryMetrics.responseTimeMs}мс")
        
        _metrics.value = _metrics.value.let { current ->
            current.copy(
                totalPromptTokens = current.totalPromptTokens + summaryMetrics.promptTokens,
                totalCompletionTokens = current.totalCompletionTokens + summaryMetrics.completionTokens,
                totalTokens = current.totalTokens + summaryMetrics.totalTokens
            )
        }
    }
    
    /**
     * Обновляет счетчик сообщений
     * @param messageCount новое количество сообщений
     */
    override suspend fun updateMessageCount(messageCount: Int) {
        logger.d("Обновлено количество сообщений: $messageCount")
        
        _metrics.value = _metrics.value.copy(messageCount = messageCount)
    }
    
    /**
     * Увеличивает счетчик сжатий
     */
    override suspend fun incrementCompressionCount() {
        val newCount = _metrics.value.compressionCount + 1
        logger.d("Увеличен счетчик сжатий: $newCount")
        
        _metrics.value = _metrics.value.copy(compressionCount = newCount)
    }
    
    /**
     * Сбрасывает все метрики
     */
    override suspend fun resetMetrics() {
        logger.d("Сброс всех метрик")
        
        _metrics.value = ChatMetrics()
    }
}
