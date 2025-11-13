package ru.izhxx.aichallenge.domain.repository

import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.summary.DialogSummaryMetrics

/**
 * Репозиторий для работы с суммаризацией диалогов
 */
interface DialogSummaryRepository {
    /**
     * Создает суммаризацию диалога на основе списка сообщений
     * @param messages список сообщений для суммаризации
     * @return результат с суммаризацией диалога и метриками использования или ошибкой
     */
    suspend fun createSummary(messages: List<LLMMessage>): Result<Pair<String, DialogSummaryMetrics>>
}
