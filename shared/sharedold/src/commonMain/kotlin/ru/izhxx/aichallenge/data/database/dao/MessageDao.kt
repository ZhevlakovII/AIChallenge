package ru.izhxx.aichallenge.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import ru.izhxx.aichallenge.data.database.entity.MessageEntity

/**
 * DAO для работы с сообщениями в базе данных
 */
@Dao
interface MessageDao {
    /**
     * Вставляет сообщение в базу данных. Если сообщение с таким ID уже существует,
     * оно будет заменено
     */
    @Upsert()
    suspend fun insertMessage(message: MessageEntity)
    
    /**
     * Вставляет список сообщений в базу данных. Если сообщение с таким ID уже существует,
     * оно будет заменено
     */
    @Insert(entity = MessageEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    /**
     * Возвращает все сообщения диалога, отсортированные по порядковому номеру
     */
    @Query("SELECT * FROM messages WHERE dialogId = :dialogId ORDER BY orderInDialog ASC")
    suspend fun getMessagesByDialogId(dialogId: String): List<MessageEntity>
    
    /**
     * Возвращает количество сообщений в диалоге
     */
    @Query("SELECT COUNT(*) FROM messages WHERE dialogId = :dialogId")
    suspend fun getMessagesCountByDialogId(dialogId: String): Int
    
    /**
     * Удаляет все сообщения диалога
     */
    @Query("DELETE FROM messages WHERE dialogId = :dialogId")
    suspend fun deleteMessagesByDialogId(dialogId: String)
    
    /**
     * Удаляет сообщение по ID
     */
    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessageById(messageId: String)
}
