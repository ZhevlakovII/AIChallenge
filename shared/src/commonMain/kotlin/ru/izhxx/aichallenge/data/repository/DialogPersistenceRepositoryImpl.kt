package ru.izhxx.aichallenge.data.repository

import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.data.database.AppDatabase
import ru.izhxx.aichallenge.data.database.entity.ChatHistoryEntity
import ru.izhxx.aichallenge.data.database.entity.DialogEntity
import ru.izhxx.aichallenge.data.database.entity.MessageEntity
import ru.izhxx.aichallenge.data.database.entity.SummaryEntity
import ru.izhxx.aichallenge.domain.model.DialogInfo
import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.summary.DialogSummaryMetrics
import ru.izhxx.aichallenge.domain.repository.DialogPersistenceRepository
import java.util.UUID

/**
 * Реализация репозитория для работы с сохраненными диалогами
 */
class DialogPersistenceRepositoryImpl(
    private val database: AppDatabase
) : DialogPersistenceRepository {

    // Создаем логгер
    private val logger = Logger.forClass(this)

    /**
     * Сохраняет одно сообщение в историю чата и контекст для LLM
     * 
     * Сохраняет два типа сообщений:
     * 1. Контекстные (для LLM) - хранятся в таблице messages, очищаются после саммаризации
     * 2. Исторические (для пользователя) - хранятся в таблице chat_history, сохраняются постоянно
     */
    override suspend fun saveMessage(
        dialogId: String,
        title: String,
        message: LLMMessage,
        orderInDialog: Int,
        promptTokens: Int?,
        completionTokens: Int?,
        totalTokens: Int?,
        responseTimeMs: Long?
    ) {
        logger.d("Сохранение сообщения для диалога: $dialogId, роль: ${message.role}, порядок: $orderInDialog")

        try {
            val currentTime = System.currentTimeMillis()
            val messageId = UUID.randomUUID().toString()

            // Создаем или обновляем запись о диалоге
            upsertDialog(dialogId, title, currentTime, orderInDialog)
            
            // Сохраняем сообщение в контекст (для LLM)
            saveContextMessage(messageId, dialogId, message, currentTime, orderInDialog)
            
            // Сохраняем сообщение в историю чата (для отображения пользователю)
            saveHistoryMessage(
                messageId, 
                dialogId, 
                message, 
                currentTime, 
                orderInDialog, 
                promptTokens, 
                completionTokens, 
                totalTokens, 
                responseTimeMs
            )

            logger.d("Сообщение успешно сохранено в обе таблицы: контекст и историю. ID: $messageId")
        } catch (e: Exception) {
            logger.e("Ошибка при сохранении сообщения", e)
            throw e
        }
    }

    /**
     * Обновляет или создает запись о диалоге
     */
    private suspend fun upsertDialog(
        dialogId: String,
        title: String,
        timestamp: Long,
        messageCount: Int
    ) {
        logger.d("Обновление/создание диалога: $dialogId")
        
        try {
            val existingDialog = database.dialogDao().getDialogById(dialogId)
            
            val dialogEntity = existingDialog?.copy(
                title = title,
                updatedAt = timestamp,
                messageCount = messageCount
            ) ?: DialogEntity(
                dialogId = dialogId,
                title = title,
                createdAt = timestamp,
                updatedAt = timestamp,
                messageCount = messageCount
            )

            database.dialogDao().insertDialog(dialogEntity)
            logger.d("Диалог успешно ${if (existingDialog != null) "обновлен" else "создан"}: $dialogId")
        } catch (e: Exception) {
            logger.e("Ошибка при работе с диалогом", e)
            throw e
        }
    }

    /**
     * Сохраняет контекстное сообщение (для LLM)
     */
    private suspend fun saveContextMessage(
        messageId: String,
        dialogId: String,
        message: LLMMessage,
        timestamp: Long,
        orderInDialog: Int
    ) {
        logger.d("Сохранение контекстного сообщения для диалога: $dialogId, ID: $messageId")
        
        try {
            val contextMessageEntity = MessageEntity(
                messageId = messageId,
                dialogId = dialogId,
                role = message.role.key,
                content = message.content,
                timestamp = timestamp,
                orderInDialog = orderInDialog
            )
            
            database.messageDao().insertMessage(contextMessageEntity)
            logger.d("Контекстное сообщение успешно сохранено. ID: $messageId")
        } catch (e: Exception) {
            logger.e("Ошибка при сохранении контекстного сообщения", e)
            throw e
        }
    }

    /**
     * Сохраняет историческое сообщение (для отображения пользователю)
     */
    private suspend fun saveHistoryMessage(
        messageId: String,
        dialogId: String,
        message: LLMMessage,
        timestamp: Long,
        orderInDialog: Int,
        promptTokens: Int?,
        completionTokens: Int?,
        totalTokens: Int?,
        responseTimeMs: Long?
    ) {
        logger.d("Сохранение исторического сообщения для диалога: $dialogId, ID: $messageId")
        
        try {
            val chatHistoryEntity = ChatHistoryEntity(
                messageId = messageId,
                dialogId = dialogId,
                role = message.role.key,
                content = message.content,
                timestamp = timestamp,
                orderInDialog = orderInDialog,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                totalTokens = totalTokens,
                responseTimeMs = responseTimeMs
            )
            
            database.chatHistoryDao().insertMessage(chatHistoryEntity)
            logger.d("Историческое сообщение успешно сохранено. ID: $messageId")
        } catch (e: Exception) {
            logger.e("Ошибка при сохранении исторического сообщения", e)
            throw e
        }
    }

    /**
     * Очищает таблицу сообщений контекста для диалога
     * Вызывается после успешной саммаризации диалога
     */
    override suspend fun clearContextMessages(dialogId: String) {
        logger.d("Очистка контекстных сообщений для диалога: $dialogId")

        try {
            val messagesCountBefore = database.messageDao().getMessagesCountByDialogId(dialogId)
            logger.d("Количество контекстных сообщений до очистки: $messagesCountBefore")
            
            database.messageDao().deleteMessagesByDialogId(dialogId)
            
            val messagesCountAfter = database.messageDao().getMessagesCountByDialogId(dialogId)
            logger.d("Контекстные сообщения очищены. Осталось сообщений: $messagesCountAfter")
        } catch (e: Exception) {
            logger.e("Ошибка при очистке контекстных сообщений", e)
            throw e
        }
    }

    /**
     * Сохраняет саммари диалога в базу данных
     */
    override suspend fun saveSummary(
        dialogId: String,
        content: String,
        metrics: DialogSummaryMetrics?
    ) {
        logger.d("Сохранение саммари для диалога: $dialogId")

        try {
            val summaryId = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()
            
            // Создаем сущность саммари
            val summaryEntity = SummaryEntity(
                summaryId = summaryId,
                dialogId = dialogId,
                content = content,
                createdAt = currentTime,
                promptTokens = metrics?.promptTokens,
                completionTokens = metrics?.completionTokens,
                totalTokens = metrics?.totalTokens,
                responseTimeMs = metrics?.responseTimeMs
            )

            // Сохраняем саммари
            database.summaryDao().insertSummary(summaryEntity)

            logger.d("Саммари успешно сохранено. ID: $summaryId")
        } catch (e: Exception) {
            logger.e("Ошибка при сохранении саммари", e)
            throw e
        }
    }

    /**
     * Возвращает все сообщения из истории чата (для отображения пользователю)
     * Включает метаданные (токены, время ответа и т.д.)
     */
    override suspend fun getChatHistory(dialogId: String): List<Map<String, Any?>> {
        logger.d("Получение истории чата для диалога: $dialogId")

        try {
            val chatHistoryEntities = database.chatHistoryDao().getMessagesByDialogId(dialogId)
            logger.d("Получено ${chatHistoryEntities.size} сообщений из истории чата")

            return chatHistoryEntities.map { entity ->
                mapOf(
                    "messageId" to entity.messageId,
                    "role" to entity.role,
                    "content" to entity.content,
                    "timestamp" to entity.timestamp,
                    "orderInDialog" to entity.orderInDialog,
                    "promptTokens" to entity.promptTokens,
                    "completionTokens" to entity.completionTokens,
                    "totalTokens" to entity.totalTokens,
                    "responseTimeMs" to entity.responseTimeMs
                )
            }.also {
                logger.d("Преобразовано ${it.size} сообщений для отображения")
            }
        } catch (e: Exception) {
            logger.e("Ошибка при получении истории чата", e)
            return emptyList()
        }
    }

    /**
     * Возвращает сообщения истории диалога (для восстановления UI)
     */
    override suspend fun getHistoryMessages(dialogId: String): List<LLMMessage> {
        logger.d("Получение сообщений истории для диалога: $dialogId")

        try {
            val chatHistoryEntities = database.chatHistoryDao().getMessagesByDialogId(dialogId)
            logger.d("Получено ${chatHistoryEntities.size} сообщений из истории")

            return chatHistoryEntities.map { entity ->
                LLMMessage(
                    role = MessageRole.getRole(entity.role),
                    content = entity.content
                )
            }.also {
                logger.d("Преобразовано ${it.size} сообщений истории в доменную модель")
            }
        } catch (e: Exception) {
            logger.e("Ошибка при получении сообщений истории", e)
            return emptyList()
        }
    }

    /**
     * Возвращает сообщения контекста диалога из базы данных (для отправки в LLM)
     */
    override suspend fun getContextMessages(dialogId: String): List<LLMMessage> {
        logger.d("Получение контекстных сообщений для диалога: $dialogId")

        try {
            val messageEntities = database.messageDao().getMessagesByDialogId(dialogId)
            logger.d("Получено ${messageEntities.size} контекстных сообщений")

            return messageEntities.map { entity ->
                LLMMessage(
                    role = MessageRole.getRole(entity.role),
                    content = entity.content
                )
            }.also {
                logger.d("Преобразовано ${it.size} контекстных сообщений в доменную модель")
            }
        } catch (e: Exception) {
            logger.e("Ошибка при получении контекстных сообщений", e)
            return emptyList()
        }
    }

    /**
     * Возвращает последнее саммари диалога из базы данных
     */
    override suspend fun getLatestSummary(dialogId: String): String? {
        logger.d("Получение последнего саммари для диалога: $dialogId")

        try {
            val summaryEntity = database.summaryDao().getLatestSummaryForDialog(dialogId)

            return summaryEntity?.content.also {
                logger.d("Получено ${if (it != null) "саммари длиной ${it.length} символов" else "NULL"} для диалога $dialogId")
            }
        } catch (e: Exception) {
            logger.e("Ошибка при получении саммари диалога", e)
            return null
        }
    }

    /**
     * Возвращает информацию о всех диалогах
     */
    override suspend fun getAllDialogs(): List<DialogInfo> {
        logger.d("Получение списка всех диалогов")

        try {
            val dialogEntities = database.dialogDao().getAllDialogs()
            logger.d("Получено ${dialogEntities.size} диалогов из базы данных")

            return dialogEntities.map { entity ->
                DialogInfo(
                    id = entity.dialogId,
                    title = entity.title,
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                    messageCount = entity.messageCount
                )
            }.also {
                logger.d("Преобразовано ${it.size} диалогов в доменную модель")
            }
        } catch (e: Exception) {
            logger.e("Ошибка при получении списка диалогов", e)
            return emptyList()
        }
    }

    /**
     * Удаляет диалог и все связанные с ним данные
     * (сообщения контекста, историю, саммари)
     */
    override suspend fun deleteDialog(dialogId: String) {
        logger.d("Удаление диалога: $dialogId со всеми связанными данными")

        try {
            // При удалении диалога, все связанные сущности будут удалены автоматически
            // благодаря ограничениям foreign key с CASCADE
            database.dialogDao().deleteDialogById(dialogId)
            logger.d("Диалог успешно удален: $dialogId")
        } catch (e: Exception) {
            logger.e("Ошибка при удалении диалога", e)
            throw e
        }
    }

    /**
     * Создает новый диалог
     */
    override suspend fun createNewDialog(): String {
        logger.d("Создание нового диалога")

        try {
            val dialogId = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()

            val dialogEntity = DialogEntity(
                dialogId = dialogId,
                title = "Новый диалог",
                createdAt = currentTime,
                updatedAt = currentTime,
                messageCount = 0
            )

            database.dialogDao().insertDialog(dialogEntity)
            logger.d("Создан новый диалог с ID: $dialogId")
            return dialogId
        } catch (e: Exception) {
            logger.e("Ошибка при создании нового диалога", e)
            throw e
        }
    }
}
