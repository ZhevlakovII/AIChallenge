package ru.izhxx.aichallenge.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность диалога для хранения в базе данных
 */
@Entity(tableName = "dialogs")
data class DialogEntity(
    /**
     * Уникальный идентификатор диалога
     */
    @PrimaryKey
    val dialogId: String,
    
    /**
     * Заголовок диалога
     */
    val title: String,
    
    /**
     * Время создания диалога в миллисекундах
     */
    val createdAt: Long,
    
    /**
     * Время последнего обновления диалога в миллисекундах
     */
    val updatedAt: Long,
    
    /**
     * Количество сообщений в диалоге
     */
    val messageCount: Int
)
