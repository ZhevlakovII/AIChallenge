package ru.izhxx.aichallenge.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import ru.izhxx.aichallenge.data.database.entity.ChatHistoryEntity

/**
 * DAO для работы с историей чата в базе данных
 * Используется для сохранения полной истории диалога для отображения пользователю
 */
@Dao
interface ChatHistoryDao {
    /**
     * Вставляет сообщение в историю чата
     */
    @Upsert()
    suspend fun insertMessage(message: ChatHistoryEntity)

    /**
     * Вставляет список сообщений в историю чата
     */
    @Insert(entity = ChatHistoryEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatHistoryEntity>)

    /**
     * Возвращает все сообщения диалога из истории, отсортированные по порядковому номеру
     */
    @Query("SELECT * FROM chat_history WHERE dialogId = :dialogId ORDER BY orderInDialog ASC")
    suspend fun getMessagesByDialogId(dialogId: String): List<ChatHistoryEntity>

    /**
     * Возвращает количество сообщений в истории диалога
     */
    @Query("SELECT COUNT(*) FROM chat_history WHERE dialogId = :dialogId")
    suspend fun getMessagesCountByDialogId(dialogId: String): Int

    /**
     * Удаляет все сообщения из истории диалога
     */
    @Query("DELETE FROM chat_history WHERE dialogId = :dialogId")
    suspend fun deleteMessagesByDialogId(dialogId: String)

    /**
     * Удаляет сообщение из истории по ID
     */
    @Query("DELETE FROM chat_history WHERE messageId = :messageId")
    suspend fun deleteMessageById(messageId: String)
}
