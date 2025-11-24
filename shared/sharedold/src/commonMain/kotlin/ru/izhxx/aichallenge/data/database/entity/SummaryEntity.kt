package ru.izhxx.aichallenge.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность саммари диалога для хранения в базе данных
 */
@Entity(
    tableName = "summaries",
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
        Index(value = ["dialogId", "createdAt"]) // Индекс для быстрого поиска и сортировки
    ]
)
data class SummaryEntity(
    /**
     * Уникальный идентификатор саммари
     */
    @PrimaryKey
    val summaryId: String,
    
    /**
     * Идентификатор диалога, к которому относится саммари
     */
    val dialogId: String,
    
    /**
     * Содержимое саммари
     */
    val content: String,
    
    /**
     * Время создания саммари в миллисекундах
     */
    val createdAt: Long,
    
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
