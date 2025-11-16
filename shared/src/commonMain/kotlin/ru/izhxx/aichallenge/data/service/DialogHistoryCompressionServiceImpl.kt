package ru.izhxx.aichallenge.data.service

import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.summary.DialogSummaryMetrics
import ru.izhxx.aichallenge.domain.repository.DialogSummaryRepository
import ru.izhxx.aichallenge.domain.service.DialogHistoryCompressionService

/**
 * Реализация сервиса сжатия истории диалога с использованием LLM для суммаризации
 */
class DialogHistoryCompressionServiceImpl(
    private val dialogSummaryRepository: DialogSummaryRepository
) : DialogHistoryCompressionService {

    // Создаем логгер
    private val logger = Logger.forClass(this)

    override suspend fun compressHistory(
        messages: List<LLMMessage>,
        previousSummary: String?,
        compressionThreshold: Int
    ): Pair<String?, DialogSummaryMetrics?> {
        // Фильтруем только сообщения пользователя и ассистента
        val messagesForSummary = messages.filter {
            it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT
        }

        if (messagesForSummary.isEmpty()) {
            logger.d("Нет сообщений для суммаризации после фильтрации")
            return Pair(null, null)
        }

        // Если сообщений меньше порога, возвращаем предыдущую суммаризацию (если есть)
        if (messagesForSummary.size < compressionThreshold) {
            logger.d("История диалога меньше порога сжатия ($compressionThreshold), сжатие не требуется")
            // Возвращаем предыдущую суммаризацию без изменений, если она есть
            return if (previousSummary != null) {
                logger.d("Возвращаем предыдущую суммаризацию без изменений")
                Pair(previousSummary, null)
            } else {
                Pair(null, null)
            }
        }
        
        try {
            logger.d("Создаем суммаризацию диалога (${messagesForSummary.size} сообщений)")
            
            // Используем обновленный репозиторий, который возвращает и суммаризацию, и метрики
            // Создаем новую суммаризацию для текущих сообщений
            val summaryResult = if (previousSummary != null) {
                // Если есть предыдущая суммаризация, включаем её в контекст
                logger.d("Создаем суммаризацию с учетом предыдущей суммаризации")
                dialogSummaryRepository.createSummary(
                    messages = messagesForSummary,
                    previousSummary = previousSummary
                )
            } else {
                // Если предыдущей суммаризации нет, создаем новую
                logger.d("Создаем новую суммаризацию без предыдущего контекста")
                dialogSummaryRepository.createSummary(messagesForSummary)
            }
            
            return summaryResult.fold(
                onSuccess = { (summary, metrics) ->
                    logger.d("Суммаризация создана успешно: ${summary.take(50)}${if (summary.length > 50) "..." else ""}")
                    logger.d("Метрики суммаризации: $metrics")
                    
                    // Возвращаем форматированную суммаризацию и метрики
                    Pair("ДИАЛОГОВАЯ ИСТОРИЯ: $summary", metrics)
                },
                onFailure = { error ->
                    // Логируем ошибку, но не прерываем работу
                    logger.e("Ошибка при создании суммаризации", error)
                    
                    // Возвращаем фолбэк-суммаризацию и null-метрики
                    // Не выбрасываем исключение, чтобы не прерывать весь процесс
                    Pair("ДИАЛОГОВАЯ ИСТОРИЯ: (произошла ошибка при создании резюме) ${error.message}", null)
                }
            )
        } catch (e: Exception) {
            // В случае непредвиденных ошибок также не прерываем работу
            logger.e("Непредвиденная ошибка при создании суммаризации", e)
            return Pair("ДИАЛОГОВАЯ ИСТОРИЯ: (произошла непредвиденная ошибка) ${e.message}", null)
        }
    }
}
