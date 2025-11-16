package ru.izhxx.aichallenge.domain.usecase

import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.summary.DialogSummaryMetrics

/**
 * UseCase для сжатия истории диалога
 */
interface CompressDialogHistoryUseCase {
    /**
     * Сжимает историю диалога, если количество сообщений превышает порог
     * @param messages история сообщений
     * @param previousSummary предыдущая суммаризация (если есть)
     * @return тройка: суммаризация (или null, если сжатие не требуется), обновленная история сообщений и метрики (или null)
     */
    suspend operator fun invoke(
        messages: List<LLMMessage>,
        previousSummary: String? = null
    ): Triple<String?, List<LLMMessage>, DialogSummaryMetrics?>
}
