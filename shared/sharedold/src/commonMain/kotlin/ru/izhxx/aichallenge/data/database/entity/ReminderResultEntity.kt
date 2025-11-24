package ru.izhxx.aichallenge.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность результата выполнения задачи-напоминания.
 */
@Entity(
    tableName = "reminder_results",
    foreignKeys = [
        ForeignKey(
            entity = ReminderTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("taskId"),
        Index("runAt")
    ]
)
data class ReminderResultEntity(
    /**
     * Уникальный идентификатор результата.
     */
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /**
     * Идентификатор задачи, к которой относится результат.
     */
    val taskId: Long,

    /**
     * Метка времени выполнения (мс).
     */
    val runAt: Long,

    /**
     * Статус выполнения (SUCCESS/ERROR).
     */
    val status: String,

    /**
     * Текст ответа для пользователя.
     */
    val responseText: String,

    /**
     * Сырые данные/трассировка вызовов инструментов (JSON), если есть.
     */
    val rawToolTrace: String? = null,

    /**
     * Описание ошибки, если выполнение завершилось неуспешно.
     */
    val errorMessage: String? = null
)
