package ru.izhxx.aichallenge.domain.service

import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.summary.DialogSummaryMetrics

/**
 * Сервис для сжатия истории диалога
 */
interface DialogHistoryCompressionService {
    /**
     * Сжимает историю диалога, создавая summary
     * @param messages история сообщений для сжатия
     * @param previousSummary предыдущая суммаризация (если есть)
     * @param compressionThreshold количество сообщений, после которого происходит сжатие
     * @return пара из строки суммаризации и метрик (или null, если сжатие не требуется)
     */
    suspend fun compressHistory(
        messages: List<LLMMessage>,
        previousSummary: String? = null,
        compressionThreshold: Int = 5
    ): Pair<String?, DialogSummaryMetrics?>
}
