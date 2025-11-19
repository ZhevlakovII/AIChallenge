package ru.izhxx.aichallenge.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность задачи-напоминания для хранения в базе данных.
 */
@Entity(
    tableName = "reminder_tasks",
    indices = [
        Index("enabled"),
        Index("nextRunAt")
    ]
)
data class ReminderTaskEntity(
    /**
     * Уникальный идентификатор задачи.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /**
     * Имя задачи (для отображения пользователю).
     */
    val name: String,

    /**
     * Системный промпт, используемый при выполнении задачи.
     */
    val systemPrompt: String,

    /**
     * Пользовательский промпт, отправляемый в LLM.
     */
    val userPrompt: String,

    /**
     * Единица периода (MINUTES/HOURS/DAYS).
     */
    val periodUnit: String,

    /**
     * Значение периода (> 0).
     */
    val periodValue: Int,

    /**
     * Признак активности задачи.
     */
    val enabled: Boolean,

    /**
     * Метка времени последнего запуска (мс) или null.
     */
    val lastRunAt: Long?,

    /**
     * Метка времени следующего запуска (мс) или null.
     */
    val nextRunAt: Long?,

    /**
     * Время создания (мс).
     */
    val createdAt: Long,

    /**
     * Время последнего обновления (мс).
     */
    val updatedAt: Long
)
