package ru.izhxx.aichallenge.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.izhxx.aichallenge.data.database.entity.DialogEntity

/**
 * DAO для работы с диалогами в базе данных
 */
@Dao
interface DialogDao {
    /**
     * Вставляет диалог в базу данных. Если диалог с таким ID уже существует,
     * он будет заменен
     */
    @Upsert()
    suspend fun insertDialog(dialog: DialogEntity)
    
    /**
     * Возвращает все диалоги, отсортированные по времени последнего обновления (сначала новые)
     */
    @Query("SELECT * FROM dialogs ORDER BY updatedAt DESC")
    suspend fun getAllDialogs(): List<DialogEntity>
    
    /**
     * Возвращает диалог по ID
     */
    @Query("SELECT * FROM dialogs WHERE dialogId = :dialogId")
    suspend fun getDialogById(dialogId: String): DialogEntity?
    
    /**
     * Обновляет заголовок диалога
     */
    @Query("UPDATE dialogs SET title = :title, updatedAt = :updatedAt WHERE dialogId = :dialogId")
    suspend fun updateDialogTitle(dialogId: String, title: String, updatedAt: Long)
    
    /**
     * Обновляет количество сообщений в диалоге
     */
    @Query("UPDATE dialogs SET messageCount = :messageCount, updatedAt = :updatedAt WHERE dialogId = :dialogId")
    suspend fun updateMessageCount(dialogId: String, messageCount: Int, updatedAt: Long)
    
    /**
     * Удаляет диалог по ID
     */
    @Query("DELETE FROM dialogs WHERE dialogId = :dialogId")
    suspend fun deleteDialogById(dialogId: String)
    
    /**
     * Возвращает последний обновленный диалог
     */
    @Query("SELECT * FROM dialogs ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLastUpdatedDialog(): DialogEntity?
}
