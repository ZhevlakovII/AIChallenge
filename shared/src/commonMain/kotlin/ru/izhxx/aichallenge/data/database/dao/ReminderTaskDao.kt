package ru.izhxx.aichallenge.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import ru.izhxx.aichallenge.data.database.entity.ReminderTaskEntity

/**
 * DAO для работы с задачами-напоминаниями.
 */
@Dao
interface ReminderTaskDao {

    /**
     * Создаёт или обновляет задачу.
     */
    @Upsert
    suspend fun upsertTask(task: ReminderTaskEntity): Long

    /**
     * Массовое создание/обновление задач.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<ReminderTaskEntity>)

    /**
     * Удаляет задачу по идентификатору.
     */
    @Query("DELETE FROM reminder_tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Возвращает все задачи.
     */
    @Query("SELECT * FROM reminder_tasks ORDER BY createdAt DESC")
    suspend fun getAll(): List<ReminderTaskEntity>

    /**
     * Возвращает задачу по идентификатору.
     */
    @Query("SELECT * FROM reminder_tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReminderTaskEntity?

    /**
     * Возвращает задачи, срок запуска которых наступил.
     */
    @Query(
        """
        SELECT * FROM reminder_tasks 
        WHERE enabled = 1 
          AND nextRunAt IS NOT NULL 
          AND nextRunAt <= :nowMillis
        ORDER BY nextRunAt ASC
        """
    )
    suspend fun getDueTasks(nowMillis: Long): List<ReminderTaskEntity>

    /**
     * Обновляет планирование запуска для задачи.
     */
    @Query(
        """
        UPDATE reminder_tasks 
        SET lastRunAt = :lastRunAt, nextRunAt = :nextRunAt, updatedAt = :updatedAt
        WHERE id = :taskId
        """
    )
    suspend fun updateSchedule(taskId: Long, lastRunAt: Long?, nextRunAt: Long?, updatedAt: Long)
}
