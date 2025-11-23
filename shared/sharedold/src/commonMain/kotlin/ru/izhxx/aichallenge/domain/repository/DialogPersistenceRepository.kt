package ru.izhxx.aichallenge.domain.repository

import ru.izhxx.aichallenge.domain.model.DialogInfo
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.summary.DialogSummaryMetrics

/**
 * Репозиторий для работы с сохраненными диалогами
 */
interface DialogPersistenceRepository {
    /**
     * Сохраняет одно сообщение в историю чата и контекст для LLM
     * @param dialogId идентификатор диалога
     * @param title заголовок диалога
     * @param message сообщение для сохранения
     * @param orderInDialog порядковый номер сообщения в диалоге
     * @param promptTokens количество токенов в запросе (опционально)
     * @param completionTokens количество токенов в ответе (опционально)
     * @param totalTokens общее количество токенов (опционально)
     * @param responseTimeMs время выполнения запроса в миллисекундах (опционально)
     */
    suspend fun saveMessage(
        dialogId: String,
        title: String,
        message: LLMMessage,
        orderInDialog: Int,
        promptTokens: Int? = null,
        completionTokens: Int? = null,
        totalTokens: Int? = null,
        responseTimeMs: Long? = null
    )

    /**
     * Очищает таблицу сообщений контекста для диалога
     * @param dialogId идентификатор диалога
     */
    suspend fun clearContextMessages(dialogId: String)
    
    /**
     * Сохраняет саммари диалога
     * @param dialogId идентификатор диалога
     * @param content содержимое саммари
     * @param metrics метрики саммари
     */
    suspend fun saveSummary(dialogId: String, content: String, metrics: DialogSummaryMetrics?)

    /**
     * Возвращает все сообщения из истории чата (для отображения пользователю)
     * @param dialogId идентификатор диалога
     * @return список сообщений с метаданными
     */
    suspend fun getChatHistory(dialogId: String): List<Map<String, Any?>>

    /**
     * Возвращает сообщения истории диалога
     * @param dialogId идентификатор диалога
     * @return список сообщений диалога
     */
    suspend fun getHistoryMessages(dialogId: String): List<LLMMessage>

    /**
     * Возвращает сообщения контекста диалога (для отправки в LLM)
     * @param dialogId идентификатор диалога
     * @return список сообщений диалога
     */
    suspend fun getContextMessages(dialogId: String): List<LLMMessage>    

    /**
     * Возвращает последнее саммари диалога
     * @param dialogId идентификатор диалога
     * @return содержимое последнего саммари или null, если саммари нет
     */
    suspend fun getLatestSummary(dialogId: String): String?
    
    /**
     * Возвращает список информации о всех диалогах
     * @return список информации о диалогах (ID, заголовок, дата создания и обновления)
     */
    suspend fun getAllDialogs(): List<DialogInfo>
    
    /**
     * Удаляет диалог с сообщениями и саммари
     * @param dialogId идентификатор диалога
     */
    suspend fun deleteDialog(dialogId: String)
    
    /**
     * Создает новый диалог
     * @return идентификатор нового диалога
     */
    suspend fun createNewDialog(): String
}
