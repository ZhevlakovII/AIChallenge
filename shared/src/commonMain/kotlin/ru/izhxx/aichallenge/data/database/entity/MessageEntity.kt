package ru.izhxx.aichallenge.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность сообщения для хранения в базе данных
 */
@Entity(
    tableName = "messages",
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
data class MessageEntity(
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
    val orderInDialog: Int
)
