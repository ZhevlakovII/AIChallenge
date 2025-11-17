package ru.izhxx.aichallenge.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность истории чата для хранения всех сообщений диалога
 * Используется для отображения пользователю и сохранения полной истории
 */
@Entity(
    tableName = "chat_history",
    foreignKeys = [
        ForeignKey(
            entity = DialogEntity::class,
            parentColumns = ["dialogId"],
            childColumns = ["dialogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("dialogId"),
        Index(value = ["dialogId", "orderInDialog"]) // Уникальный индекс для предотвращения дублирования порядковых номеров
    ]
)
data class ChatHistoryEntity(
    /**
     * Уникальный идентификатор сообщения
     */
    @PrimaryKey
    val messageId: String,

    /**
     * Идентификатор диалога, которому принадлежит сообщение
     */
    val dialogId: String,

    /**
     * Роль отправителя сообщения (USER, ASSISTANT, SYSTEM, TECH)
     */
    val role: String,

    /**
     * Содержимое сообщения
     */
    val content: String,

    /**
     * Время отправки сообщения в миллисекундах
     */
    val timestamp: Long,

    /**
     * Порядковый номер сообщения в диалоге
     * Используется для сохранения порядка сообщений
     */
    val orderInDialog: Int,

    /**
     * Количество токенов в запросе
     * Может быть null, если метрики недоступны
     */
    val promptTokens: Int?,
    
    /**
     * Количество токенов в ответе
     * Может быть null, если метрики недоступны
     */
    val completionTokens: Int?,
    
    /**
     * Общее количество токенов
     * Может быть null, если метрики недоступны
     */
    val totalTokens: Int?,
    
    /**
     * Время выполнения запроса в миллисекундах
     * Может быть null, если метрики недоступны
     */
    val responseTimeMs: Long?
)
