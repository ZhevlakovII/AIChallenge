package ru.izhxx.aichallenge.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.izhxx.aichallenge.data.database.entity.ReminderResultEntity

/**
 * DAO для работы с результатами выполнения напоминаний.
 */
@Dao
interface ReminderResultDao {

    /**
     * Вставляет запись о результате выполнения задачи.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(result: ReminderResultEntity): Long

    /**
     * Возвращает результаты по задаче с пагинацией.
     */
    @Query(
        """
        SELECT * FROM reminder_results 
        WHERE taskId = :taskId
        ORDER BY runAt DESC
        LIMIT :limit OFFSET :offset
        """
    )
    suspend fun getByTask(taskId: Long, limit: Int, offset: Int): List<ReminderResultEntity>

    /**
     * Возвращает последние результаты (по всем задачам).
     */
    @Query(
        """
        SELECT * FROM reminder_results 
        ORDER BY runAt DESC
        LIMIT :limit
        """
    )
    suspend fun getLatest(limit: Int): List<ReminderResultEntity>
}
