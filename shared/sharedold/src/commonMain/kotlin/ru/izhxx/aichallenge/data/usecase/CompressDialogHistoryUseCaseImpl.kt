package ru.izhxx.aichallenge.data.usecase

import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.summary.DialogSummaryMetrics
import ru.izhxx.aichallenge.domain.service.DialogHistoryCompressionService
import ru.izhxx.aichallenge.domain.usecase.CompressDialogHistoryUseCase

/**
 * Реализация UseCase для сжатия истории диалога
 */
class CompressDialogHistoryUseCaseImpl(
    private val compressionService: DialogHistoryCompressionService
) : CompressDialogHistoryUseCase {

    // Создаем логгер
    private val logger = Logger.forClass(this)

    // Порог количества сообщений для сжатия
    private val compressionThreshold = 5

    override suspend fun invoke(
        messages: List<LLMMessage>,
        previousSummary: String?
    ): Triple<String?, List<LLMMessage>, DialogSummaryMetrics?> {
        // Получаем суммаризацию и метрики от сервиса, передавая предыдущую суммаризацию
        val (summary, metrics) = compressionService.compressHistory(
            messages = messages,
            previousSummary = previousSummary,
            compressionThreshold = compressionThreshold
        )

        // Если суммаризация не была создана, возвращаем оригинальную историю без метрик
        if (summary == null) {
            logger.d("Суммаризация не была создана, возвращаем оригинальные сообщения")
            return Triple(null, messages, null)
        }

        logger.d("Создана суммаризация, сжато с ${messages.size} до 0 сообщений")
        if (metrics != null) {
            logger.d("Метрики суммаризации: $metrics")
        }

        // Возвращаем тройку: суммаризация, сжатые сообщения и метрики
        return Triple(summary, emptyList(), metrics)
    }
}
