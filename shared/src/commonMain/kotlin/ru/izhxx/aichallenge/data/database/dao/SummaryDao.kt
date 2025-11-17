package ru.izhxx.aichallenge.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.izhxx.aichallenge.data.database.entity.SummaryEntity

/**
 * DAO для работы с саммари в базе данных
 */
@Dao
interface SummaryDao {
    /**
     * Вставляет саммари в базу данных. Если саммари с таким ID уже существует,
     * оно будет заменено
     */
    @Insert(entity = SummaryEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: SummaryEntity)
    
    /**
     * Возвращает последнее саммари для диалога
     */
    @Query("SELECT * FROM summaries WHERE dialogId = :dialogId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestSummaryForDialog(dialogId: String): SummaryEntity?
    
    /**
     * Возвращает все саммари для диалога, отсортированные по времени создания (сначала старые)
     */
    @Query("SELECT * FROM summaries WHERE dialogId = :dialogId ORDER BY createdAt ASC")
    suspend fun getAllSummariesForDialog(dialogId: String): List<SummaryEntity>
    
    /**
     * Удаляет все саммари для диалога
     */
    @Query("DELETE FROM summaries WHERE dialogId = :dialogId")
    suspend fun deleteSummariesForDialog(dialogId: String)
    
    /**
     * Возвращает общее количество токенов, использованных для саммари диалога
     */
    @Query("SELECT SUM(totalTokens) FROM summaries WHERE dialogId = :dialogId")
    suspend fun getTotalTokensForDialog(dialogId: String): Int?
    
    /**
     * Возвращает общее количество времени, затраченное на создание саммари диалога
     */
    @Query("SELECT SUM(responseTimeMs) FROM summaries WHERE dialogId = :dialogId")
    suspend fun getTotalResponseTimeForDialog(dialogId: String): Long?
}
